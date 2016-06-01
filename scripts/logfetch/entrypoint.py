import argparse
import ConfigParser
import sys
import os
import pkg_resources
import time
from datetime import datetime, timedelta
from termcolor import colored
from fake_section_head import FakeSectionHead
from live_logs import download_live_logs
from s3_logs import download_s3_logs
from search import find_cached_logs
from tail import start_tail
from grep import grep_files
from cat import cat_files

VERSION = pkg_resources.require("singularity-logfetch")[0].version
CONF_READ_ERR_FORMAT = 'Could not load config from {0} due to {1}'
DEFAULT_CONF_DIR = os.path.expanduser('~/.logfetch')
DEFAULT_CONF_FILE = 'default'
DEFAULT_PARALLEL_FETCHES = 10
DEFAULT_CHUNK_SIZE = 8192
DEFAULT_DEST = os.path.expanduser('~/.logfetch_cache')
DEFAULT_TASK_COUNT = 20
DEFAULT_DAYS = 7
DEFAULT_S3_PATTERN = '%requestId/%%Y/%m/%taskId_%index-%s-%filename'

IS_A_TTY = sys.stdout.isatty()

def exit(reason, color='red'):
    sys.stderr.write(colored(reason, color) + '\n')
    sys.exit(1)

def tail_logs(args):
    try:
        start_tail(args)
    except KeyboardInterrupt:
        exit('Stopping logtail...', 'magenta')

def search_logs(args):
    try:
        all_logs = find_cached_logs(args)
        grep_files(args, all_logs)
    except KeyboardInterrupt:
        exit('Stopping logfetch...', 'magenta')

def fetch_logs(args):
    try:
        check_dest(args)
        all_logs = []
        if not args.skip_s3:
            all_logs += download_s3_logs(args)
        if not args.skip_live:
            all_logs += download_live_logs(args)
        if not args.download_only:
            grep_files(args, all_logs)
    except KeyboardInterrupt:
        exit('Stopping logfetch...', 'magenta')

def cat_logs(args):
    try:
        check_dest(args)
        all_logs = []
        if not args.skip_s3:
            all_logs += download_s3_logs(args)
        if not args.skip_live:
            all_logs += download_live_logs(args)
        if not args.download_only:
            cat_files(args, all_logs)
    except KeyboardInterrupt:
        exit('Stopping logcat...', 'magenta')


def check_dest(args):
    if not os.path.exists(args.dest):
        os.makedirs(args.dest)

def check_args(args):
    if args.deployId and not args.requestId:
        exit("Must specify request-id (-r) when specifying deploy-id")
    elif not args.requestId and not args.deployId and not args.taskId:
        exit('Must specify one of\n -t task-id\n -r request-id and -d deploy-id\n -r request-id')

def convert_to_date(args, argument, is_start):
    try:
        if isinstance(argument, datetime):
            return argument
        else:
            val = datetime.utcnow() - timedelta(days=int(argument))
    except:
        try:
            if args.zone:
                timestring = '{0} {1}'.format(argument, '00:00:00' if is_start else '23:59:59') if len(argument) < 11 else argument
                val = datetime.utcfromtimestamp(time.mktime(datetime.strptime(timestring, "%Y-%m-%d %H:%M:%S").timetuple()))
            else:
                timestring = '{0} {1}'.format(argument, '00:00:00' if is_start else '23:59:59') if len(argument) < 11 else argument
                val = datetime.strptime('{0} UTC'.format(timestring), "%Y-%m-%d %H:%M:%S %Z")
        except:
            exit('Start/End days value must be either a number of days or a date in format "%%Y-%%m-%%d %%H:%%M:%%S" or "%%Y-%%m-%%d"')
    return val

def fetch():
    conf_parser = argparse.ArgumentParser(version=VERSION, description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter, add_help=False)
    conf_parser.add_argument("-f", "--conf-folder", dest='conf_folder', help="specify a folder for config files to live")
    conf_parser.add_argument("-c", "--conf-file", dest='conf_file', help="Specify config file within the conf folder", metavar="FILE")
    args, remaining_argv = conf_parser.parse_known_args()
    conf_dir = args.conf_folder if args.conf_folder else DEFAULT_CONF_DIR
    conf_file = os.path.expanduser(conf_dir + '/' + args.conf_file) if args.conf_file else os.path.expanduser(conf_dir + '/' + DEFAULT_CONF_FILE)
    config = ConfigParser.SafeConfigParser()
    config.optionxform = str

    defaults = {
        "num_parallel_fetches" : DEFAULT_PARALLEL_FETCHES,
        "chunk_size" : DEFAULT_CHUNK_SIZE,
        "dest" : DEFAULT_DEST,
        "task_count" : DEFAULT_TASK_COUNT,
        "start" : datetime.strptime('{0} 00:00:00'.format(datetime.now().strftime("%Y-%m-%d")), "%Y-%m-%d %H:%M:%S") - timedelta(days=DEFAULT_DAYS),
        "file_pattern" : DEFAULT_S3_PATTERN,
        "end" : datetime.strptime('{0} 23:59:59'.format(datetime.now().strftime("%Y-%m-%d")), "%Y-%m-%d %H:%M:%S")
    }

    try:
        config.readfp(FakeSectionHead(open(os.path.expanduser(conf_file))))
        defaults.update(dict(config.items("Defaults")))
    except Exception, err:
        sys.stderr.write(CONF_READ_ERR_FORMAT.format(conf_file, err) + '\n')

    parser = argparse.ArgumentParser(parents=[conf_parser], description="Fetch log files from Singularity. One can specify either a TaskId, RequestId and DeployId, or RequestId", prog="logfetch")

    parser.set_defaults(**defaults)
    parser.add_argument("-t", "--task-id", dest="taskId", help="TaskId of task to fetch logs for")
    parser.add_argument("-r", "--request-id", dest="requestId", help="RequestId of request to fetch logs for (can be a glob)")
    parser.add_argument("-T", "--task-count", dest="task_count", help="Number of recent tasks per request to fetch logs from", type=int)
    parser.add_argument("-d", "--deploy-id", dest="deployId", help="DeployId of task to fetch logs for (can be a glob)")
    parser.add_argument("-o", "--dest", dest="dest", help="Destination directory")
    parser.add_argument("-n", "--num-parallel-fetches", dest="num_parallel_fetches", help="Number of fetches to make at once", type=int)
    parser.add_argument("-C", "--chunk-size", dest="chunk_size", help="Chunk size for writing from response to filesystem", type=int)
    parser.add_argument("-u", "--singularity-uri-base", dest="singularity_uri_base", help="The base for singularity (eg. http://localhost:8080/singularity/v1)")
    parser.add_argument("-s", "--start", dest="start", help="Search for logs no older than this, can be an integer number of days or date in format '%%Y-%%m-%%d %%H:%%M:%%S' or '%%Y-%%m-%%d'")
    parser.add_argument("-e", "--end", dest="end", help="Search for logs no newer than this, can be an integer number of days or date in format '%%Y-%%m-%%d %%H:%%M:%%S' or '%%Y-%%m-%%d' (defaults to None/now)")
    parser.add_argument("-l", "--log-type", dest="logtype", help="Logfile type to downlaod (ie 'access.log'), can be a glob (ie *.log)")
    parser.add_argument("-p", "--file-pattern", dest="file_pattern", help="S3 uploader file pattern")
    parser.add_argument("-N", "--no-name-fetch-off", dest="no_name_fetch_off", help="If a logtype matcher is specified, but the s3 log pattern does not include file name, don't download any s3 files", action="store_true")
    parser.add_argument("-g", "--grep", dest="grep", help="Regex to grep for (normal grep syntax) or a full grep command")
    parser.add_argument("-z", "--local-zone", dest="zone", help="If specified, input times in the local time zone and convert to UTC, if not specified inputs are assumed to be UTC", action="store_true")
    parser.add_argument("-S", "--skip-s3", dest="skip_s3", help="Don't download/search s3 logs", action='store_true')
    parser.add_argument("-L", "--skip-live", dest="skip_live", help="Don't download/search live logs", action='store_true')
    parser.add_argument("-U", "--use-cache", dest="use_cache", help="Use cache for live logs, don't re-download them", action='store_true')
    parser.add_argument("--search", dest="search", help="run logsearch on the local cache of downloaded files", action='store_true')
    parser.add_argument("-V", "--verbose", dest="verbose", help="Print more verbose output", action='store_true')
    parser.add_argument("--silent", dest="silent", help="No stderr (progress, file names, etc) output", action='store_true')
    parser.add_argument("-D" ,"--download-only", dest="download_only", help="Only download files, don't unzip or grep", action='store_true')

    args = parser.parse_args(remaining_argv)

    if not IS_A_TTY:
        args.silent = True

    check_args(args)
    args.start = convert_to_date(args, args.start, True)
    args.end = convert_to_date(args, args.end, False)

    args.dest = os.path.expanduser(args.dest)
    try:
        setattr(args, 'headers', dict(config.items("Request Headers")))
    except:
        if not args.silent:
            sys.stderr.write('No additional request headers found\n')
        setattr(args, 'headers', {})

    if args.search:
        search_logs(args)
    else:
        fetch_logs(args)

def search():
    conf_parser = argparse.ArgumentParser(version=VERSION, description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter, add_help=False)
    conf_parser.add_argument("-f", "--conf-folder", dest='conf_folder', help="specify a folder for config files to live")
    conf_parser.add_argument("-c", "--conf-file", dest='conf_file', help="Specify config file within the conf folder", metavar="FILE")
    args, remaining_argv = conf_parser.parse_known_args()
    conf_dir = args.conf_folder if args.conf_folder else DEFAULT_CONF_DIR
    conf_file = os.path.expanduser(conf_dir + '/' + args.conf_file) if args.conf_file else os.path.expanduser(conf_dir + '/' + DEFAULT_CONF_FILE)
    config = ConfigParser.SafeConfigParser()
    config.optionxform = str

    defaults = {
        "dest" : DEFAULT_DEST,
        "start" : datetime.strptime('{0} 00:00:00'.format(datetime.now().strftime("%Y-%m-%d")), "%Y-%m-%d %H:%M:%S") - timedelta(days=DEFAULT_DAYS),
        "file_pattern" : DEFAULT_S3_PATTERN,
        "end" : datetime.strptime('{0} 23:59:59'.format(datetime.now().strftime("%Y-%m-%d")), "%Y-%m-%d %H:%M:%S")
    }

    try:
        config.readfp(FakeSectionHead(open(os.path.expanduser(conf_file))))
        defaults.update(dict(config.items("Defaults")))
    except Exception, err:
        sys.stderr.write(CONF_READ_ERR_FORMAT.format(conf_file, err) + '\n')

    parser = argparse.ArgumentParser(parents=[conf_parser], description="Search log files in the cache directory", prog="logsearch")

    parser.set_defaults(**defaults)
    parser.add_argument("-t", "--task-id", dest="taskId", help="TaskId of task to fetch logs for")
    parser.add_argument("-r", "--request-id", dest="requestId", help="RequestId of request to fetch logs for (can be a glob)")
    parser.add_argument("-d", "--deploy-id", dest="deployId", help="DeployId of task to fetch logs for (can be a glob)")
    parser.add_argument("-o", "--dest", dest="dest", help="Destination directory")
    parser.add_argument("-s", "--start", dest="start", help="Search for logs no older than this, can be an integer number of days or date in format '%%Y-%%m-%%d %%H:%%M:%%S' or '%%Y-%%m-%%d'")
    parser.add_argument("-e", "--end", dest="end", help="Search for logs no newer than this, can be an integer number of days or date in format '%%Y-%%m-%%d %%H:%%M:%%S' or '%%Y-%%m-%%d' (defaults to None/now)")
    parser.add_argument("-l", "--log-type", dest="logtype", help="Logfile type to downlaod (ie 'access.log'), can be a glob (ie *.log)")
    parser.add_argument("-p", "--file-pattern", dest="file_pattern", help="S3 uploader file pattern")
    parser.add_argument("-g", "--grep", dest="grep", help="Regex to grep for (normal grep syntax) or a full grep command")
    parser.add_argument("-z", "--local-zone", dest="zone", help="If specified, input times in the local time zone and convert to UTC, if not specified inputs are assumed to be UTC", action="store_true")
    parser.add_argument("-V", "--verbose", dest="verbose", help="Print more verbose output", action='store_true')
    parser.add_argument("--silent", dest="silent", help="No stderr (progress, file names, etc) output", action='store_true')

    args, unknown = parser.parse_known_args(remaining_argv)

    if not IS_A_TTY:
        args.silent = True

    if args.verbose and unknown:
        if not args.silent:
            sys.stderr.write(colored('Found unknown args {0}'.format(unknown), 'magenta'))

    check_args(args)
    args.start = convert_to_date(args, args.start, True)
    args.end = convert_to_date(args, args.end, False)

    args.dest = os.path.expanduser(args.dest)

    search_logs(args)

def cat():
    conf_parser = argparse.ArgumentParser(version=VERSION, description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter, add_help=False)
    conf_parser.add_argument("-f", "--conf-folder", dest="conf_folder", help="specify a folder for config files to live")
    conf_parser.add_argument("-c", "--conf-file", dest="conf_file", help="Specify config file within the conf folder", metavar="FILE")
    args, remaining_argv = conf_parser.parse_known_args()
    conf_dir = args.conf_folder if args.conf_folder else DEFAULT_CONF_DIR
    conf_file = os.path.expanduser(conf_dir + '/' + args.conf_file) if args.conf_file else os.path.expanduser(conf_dir + '/' + DEFAULT_CONF_FILE)
    config = ConfigParser.SafeConfigParser()
    config.optionxform = str

    defaults = {
        "num_parallel_fetches" : DEFAULT_PARALLEL_FETCHES,
        "chunk_size" : DEFAULT_CHUNK_SIZE,
        "dest" : DEFAULT_DEST,
        "task_count" : DEFAULT_TASK_COUNT,
        "start" : datetime.strptime('{0} 00:00:00'.format(datetime.now().strftime("%Y-%m-%d")), "%Y-%m-%d %H:%M:%S") - timedelta(days=DEFAULT_DAYS),
        "file_pattern" : DEFAULT_S3_PATTERN,
        "end" : datetime.strptime('{0} 23:59:59'.format(datetime.now().strftime("%Y-%m-%d")), "%Y-%m-%d %H:%M:%S")
    }

    try:
        config.readfp(FakeSectionHead(open(os.path.expanduser(conf_file))))
        defaults.update(dict(config.items("Defaults")))
    except Exception, err:
        sys.stderr.write(CONF_READ_ERR_FORMAT.format(conf_file, err) + '\n')

    parser = argparse.ArgumentParser(parents=[conf_parser], description="Fetch log files from Singularity and cat to stdout. One can specify either a TaskId, RequestId and DeployId, or RequestId", prog="logcat")

    parser.set_defaults(**defaults)
    parser.add_argument("-t", "--task-id", dest="taskId", help="TaskId of task to fetch logs for")
    parser.add_argument("-r", "--request-id", dest="requestId", help="RequestId of request to fetch logs for (can be a glob)")
    parser.add_argument("-T", "--task-count", dest="taskCount", help="Number of recent tasks per request to fetch logs from", type=int)
    parser.add_argument("-d", "--deploy-id", dest="deployId", help="DeployId of tasks to fetch logs for (can be a glob)")
    parser.add_argument("-o", "--dest", dest="dest", help="Destination directory")
    parser.add_argument("-n", "--num-parallel-fetches", dest="num_parallel_fetches", help="Number of fetches to make at once", type=int)
    parser.add_argument("-C", "--chunk-size", dest="chunk_size", help="Chunk size for writing from response to filesystem", type=int)
    parser.add_argument("-u", "--singularity-uri-base", dest="singularity_uri_base", help="The base for singularity (eg. http://localhost:8080/singularity/v1)")
    parser.add_argument("-s", "--start", dest="start", help="Search for logs no older than this, can be an integer number of days or date in format '%%Y-%%m-%%d %%H:%%M:%%S' or '%%Y-%%m-%%d'")
    parser.add_argument("-e", "--end", dest="end", help="Search for logs no newer than this, can be an integer number of days or date in format '%%Y-%%m-%%d %%H:%%M:%%S' or '%%Y-%%m-%%d' (defaults to None/now)")
    parser.add_argument("-l", "--logtype", dest="logtype", help="Logfile type to downlaod (ie 'access.log'), can be a glob (ie *.log)")
    parser.add_argument("-p", "--file-pattern", dest="file_pattern", help="S3 uploader file pattern")
    parser.add_argument("-N", "--no-name-fetch-off", dest="no_name_fetch_off", help="If a logtype matcher is specified, but the s3 log pattern does not include file name, don't download any s3 files", action="store_true")
    parser.add_argument("-z", "--local-zone", dest="zone", help="If specified, input times in the local time zone and convert to UTC, if not specified inputs are assumed to be UTC", action="store_true")
    parser.add_argument("-S", "--skip-s3", dest="skip_s3", help="Don't download/search s3 logs", action='store_true')
    parser.add_argument("-L", "--skip-live", dest="skip_live", help="Don't download/search live logs", action='store_true')
    parser.add_argument("-U", "--use-cache", dest="use_cache", help="Use cache for live logs, don't re-download them", action='store_true')
    parser.add_argument("-V", "--verbose", dest="verbose", help="Print more verbose output", action='store_true')
    parser.add_argument("--silent", dest="silent", help="No stderr (progress, file names, etc) output", action='store_true')
    parser.add_argument("-D" ,"--download-only", dest="download_only", help="Only download files, don't unzip or grep", action='store_true')

    args = parser.parse_args(remaining_argv)

    if not IS_A_TTY:
        args.silent = True

    check_args(args)
    args.start = convert_to_date(args, args.start, True)
    args.end = convert_to_date(args, args.end, False)

    args.dest = os.path.expanduser(args.dest)
    try:
        setattr(args, 'headers', dict(config.items("Request Headers")))
    except:
        if not args.silent:
            sys.stderr.write('No additional request headers found\n')
        setattr(args, 'headers', {})


    cat_logs(args)

def tail():
    conf_parser = argparse.ArgumentParser(version=VERSION, description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter, add_help=False)
    conf_parser.add_argument("-f", "--conf-folder", dest="conf_folder", help="specify a folder for config files to live")
    conf_parser.add_argument("-c", "--conf-file", dest="conf_file", help="Specify config file within the conf folder", metavar="FILE")
    args, remaining_argv = conf_parser.parse_known_args()
    conf_dir = args.conf_folder if args.conf_folder else DEFAULT_CONF_DIR
    conf_file = os.path.expanduser(conf_dir + '/' + args.conf_file) if args.conf_file else os.path.expanduser(conf_dir + '/' + DEFAULT_CONF_FILE)
    config = ConfigParser.SafeConfigParser()
    config.optionxform = str

    defaults = {'verbose': False}

    try:
        config.readfp(FakeSectionHead(open(os.path.expanduser(conf_file))))
        defaults.update(dict(config.items("Defaults")))
    except Exception, err:
        sys.stderr.write(CONF_READ_ERR_FORMAT.format(conf_file, err) + '\n')

    parser = argparse.ArgumentParser(parents=[conf_parser], description="Tail log files from Singularity. One can specify either a TaskId, RequestId and DeployId, or RequestId", prog="logtail")

    parser.set_defaults(**defaults)
    parser.add_argument("-t", "--task-id", dest="taskId", help="TaskId of task to fetch logs for")
    parser.add_argument("-r", "--request-id", dest="requestId", help="RequestId of request to fetch logs for (can be a glob)")
    parser.add_argument("-d", "--deploy-id", dest="deployId", help="DeployId of tasks to fetch logs for (can be a glob)")
    parser.add_argument("-u", "--singularity-uri-base", dest="singularity_uri_base", help="The base for singularity (eg. http://localhost:8080/singularity/v1)")
    parser.add_argument("-l", "--logfile", dest="logfile", help="Logfile path/name to tail (ie 'logs/access.log')")
    parser.add_argument("-V", "--verbose", dest="verbose", help="more verbose output", action='store_true')
    parser.add_argument("--silent", dest="silent", help="No stderr (progress, file names, etc) output", action='store_true')

    args = parser.parse_args(remaining_argv)

    if not IS_A_TTY:
        args.silent = True

    if not args.logfile:
        exit("Must specify logfile to tail (-l)")
    check_args(args)

    args.dest = os.path.expanduser(args.dest)
    try:
        setattr(args, 'headers', dict(config.items("Request Headers")))
    except:
        if not args.silent:
            sys.stderr.write('No additional request headers found\n')
        setattr(args, 'headers', {})


    tail_logs(args)
