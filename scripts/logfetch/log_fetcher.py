import argparse
import ConfigParser
import sys
import os
from termcolor import colored

from fake_section_head import FakeSectionHead
from live_logs import download_live_logs
from s3_logs import download_s3_logs
from grep import grep_files

CONF_READ_ERR_FORMAT = 'Could not load config from {0} due to {1}'
DEFAULT_CONF_DIR = os.path.expanduser('~/.logfetch')
DEFAULT_CONF_FILE = 'default'
DEFAULT_PARALLEL_FETCHES = 5
DEFAULT_CHUNK_SIZE = 8192
DEFAULT_DEST = os.path.expanduser('~/.logfetch_cache')
DEFAULT_TASK_COUNT = 1
DEFAULT_DAYS = 7

def exit(reason):
  sys.stderr.write(colored(reason, 'red') + '\n')
  sys.exit(1)

def main(args):
  check_dest(args)
  all_logs = []
  all_logs += download_s3_logs(args)
  if args.end_days and not args.end_days > 0:
    all_logs += download_live_logs(args)
  grep_files(args, all_logs)

def check_dest(args):
  if not os.path.exists(args.dest):
    os.makedirs(args.dest)

def entrypoint():
  conf_parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter, add_help=False)
  conf_parser.add_argument("-f", "--conf_folder", help="specify a folder for config files to live")
  conf_parser.add_argument("-c", "--conf_file", help="Specify config file within the conf folder", metavar="FILE")
  args, remaining_argv = conf_parser.parse_known_args()
  conf_dir = args.conf_folder if args.conf_folder else DEFAULT_CONF_DIR
  conf_file = os.path.expanduser(conf_dir + '/' + args.conf_file) if args.conf_file else os.path.expanduser(conf_dir + '/' + DEFAULT_CONF_FILE)
  config = ConfigParser.SafeConfigParser()

  defaults = {
    "num_parallel_fetches" : DEFAULT_PARALLEL_FETCHES,
    "chunk_size" : DEFAULT_CHUNK_SIZE,
    "dest" : DEFAULT_DEST,
    "task_count" : DEFAULT_TASK_COUNT,
    "start_days" : DEFAULT_DAYS
  }

  try:
    config.readfp(FakeSectionHead(open(os.path.expanduser(conf_file))))
    defaults.update(dict(config.items("Defaults")))
  except Exception, err:
    sys.stderr.write(CONF_READ_ERR_FORMAT.format(conf_file, err) + '\n')

  parser = argparse.ArgumentParser(parents=[conf_parser], description="Fetch log files from Singularity. One can specify either a TaskId, RequestId and DeployId, or RequestId",
          prog="log_fetcher")

  parser.set_defaults(**defaults)
  parser.add_argument("-t", "--taskId", help="TaskId of task to fetch logs for", metavar="taskId")
  parser.add_argument("-r", "--requestId", help="RequestId of request to fetch logs for", metavar="requestId")
  parser.add_argument("--task-count", help="Number of recent tasks per request to fetch logs from", metavar="taskCount")
  parser.add_argument("-d", "--deployId", help="DeployId of task to fetch logs for", metavar="deployId")
  parser.add_argument("--dest", help="Destination directory", metavar="DIR")
  parser.add_argument("-n", "--num-parallel-fetches", help="Number of fetches to make at once", type=int, metavar="INT")
  parser.add_argument("-cs", "--chunk-size", help="Chunk size for writing from response to filesystem", type=int, metavar="INT")
  parser.add_argument("-u", "--singularity-uri-base", help="The base for singularity (eg. http://localhost:8080/singularity/v1)", metavar="URI")
  parser.add_argument("-s", "--start-days", help="Search for logs no older than this many days", type=int, metavar="start_days")
  parser.add_argument("-e", "--end-days", help="Search for logs no new than this many days (defaults to None/today)", type=int, metavar="end_days")
  parser.add_argument("-g", "--grep", help="Regex to grep for (normal grep syntax) or a full grep command", metavar='grep')

  args = parser.parse_args(remaining_argv)

  if args.deployId and not args.requestId:
    exit("Must specify requestId (-r) when specifying deployId")
  elif not args.requestId and not args.deployId and not args.taskId:
    exit('Must specify one of\n -t taskId\n -r requestId and -d deployId\n -r requestId')

  args.dest = os.path.expanduser(args.dest)

  main(args)
