import os
import sys
import grequests
import logfetch_base
import time
from termcolor import colored
import callbacks

TASK_FORMAT = '/task/{0}'
S3LOGS_URI_FORMAT = '{0}/logs{1}'
REQUEST_FORMAT = '/request/{0}'

FILE_REGEX="\d{13}-([^-]*)-\d{8,20}\.gz"

progress = 0
goal = 0

def download_s3_logs(args):
    if not args.silent:
        sys.stderr.write(colored('Checking for S3 log files', 'cyan') + '\n')
    callbacks.progress = 0
    logs = logs_for_all_requests(args)
    async_requests = []
    all_logs = []
    for log_file in logs:
        filename = log_file['key'].rsplit("/", 1)[1]
        if log_file_in_date_range(args, log_file):
            if not args.logtype or log_matches(args, filename):
                logfetch_base.log(colored('Including log {0}'.format(filename), 'blue') + '\n', args, True)
                if not already_downloaded(args.dest, filename):
                    async_requests.append(
                        grequests.AsyncRequest('GET', log_file['getUrl'], callback=callbacks.generate_callback(log_file['getUrl'], args.dest, filename, args.chunk_size, args.verbose, args.silent), headers=args.headers)
                    )
                else:
                    logfetch_base.log(colored('Log already downloaded {0}'.format(filename), 'blue') + '\n', args, True)
                all_logs.append('{0}/{1}'.format(args.dest, filename))
            else:
                logfetch_base.log(colored('Excluding {0} log does not match logtype argument {1}'.format(filename, args.logtype), 'magenta') + '\n', args, True)
        else:
            logfetch_base.log(colored('Excluding {0}, not in date range'.format(filename), 'magenta') + '\n', args, True)
    if async_requests:
        logfetch_base.log(colored('Starting {0} S3 Downloads with {1} parallel fetches\n'.format(len(async_requests), args.num_parallel_fetches), 'cyan'), args, False)
        callbacks.goal = len(async_requests)
        grequests.map(async_requests, stream=True, size=args.num_parallel_fetches)
    else:
        logfetch_base.log(colored('No S3 logs to download\n', 'cyan'), args, False)
    logfetch_base.log(colored('All S3 logs up to date\n', 'cyan'), args, False)
    all_logs = modify_download_list(all_logs)
    return all_logs

def log_file_in_date_range(args, log_file):
    if 'startTime' in log_file:
        if 'endTime' in log_file:
            return logfetch_base.date_range_overlaps(args, int(str(log_file['startTime'])[0:-3]), int(str(log_file['endTime'])[0:-3]))
        else:
            return logfetch_base.date_range_overlaps(args, int(str(log_file['startTime'])[0:-3]), int(str(log_file['lastModified'])[0:-3]))
    elif 'endTime' in log_file:
        return logfetch_base.is_in_date_range(args, int(str(log_file['endTime'])[0:-3]))
    else:
        return logfetch_base.is_in_date_range(args, int(str(log_file['lastModified'])[0:-3]))

def modify_download_list(all_logs):
    for index, log in enumerate(all_logs):
        if log.endswith('.gz') and not os.path.isfile(log) and os.path.isfile(log[:-3]):
            all_logs[index] = log[:-3]
    return all_logs


def already_downloaded(dest, filename):
    return (os.path.isfile('{0}/{1}'.format(dest, filename.replace('.gz', '.log'))) or os.path.isfile('{0}/{1}'.format(dest, filename[:-3])) or os.path.isfile('{0}/{1}'.format(dest, filename)))

def logs_for_all_requests(args):
    s3_params = {'start': int(time.mktime(args.start.timetuple()) * 1000), 'end': int(time.mktime(args.end.timetuple()) * 1000)}
    if args.taskId:
        return logfetch_base.get_json_response(s3_task_logs_uri(args, args.taskId), args, s3_params)
    else:
        tasks = logfetch_base.tasks_for_requests(args)
        logs = []
        tasks_progress = 0
        tasks_goal = len(tasks)
        for task in tasks:
            s3_logs = logfetch_base.get_json_response(s3_task_logs_uri(args, task), args, s3_params)
            logs = logs + s3_logs if s3_logs else logs
            tasks_progress += 1
            logfetch_base.update_progress_bar(tasks_progress, tasks_goal, 'S3 Log Finder', args.silent or args.verbose)
        logfetch_base.log(colored('\nAlso searching s3 history...\n', 'cyan'), args, False)
        for request in logfetch_base.all_requests(args):
            s3_logs = logfetch_base.get_json_response(s3_request_logs_uri(args, request), args, s3_params)
            logs = logs + s3_logs if s3_logs else logs
        found_logs = []
        keys = []
        for log in logs:
            if not log['key'] in keys:
                found_logs.append(log)
                keys.append(log['key'])
        return found_logs

def s3_task_logs_uri(args, idString):
    return S3LOGS_URI_FORMAT.format(logfetch_base.base_uri(args), TASK_FORMAT.format(idString))

def s3_request_logs_uri(args, idString):
    return S3LOGS_URI_FORMAT.format(logfetch_base.base_uri(args), REQUEST_FORMAT.format(idString))

def log_matches(args, filename):
    if 'filename' in args.file_pattern:
        return logfetch_base.log_matches(filename, '*{0}*'.format(args.logtype.replace('logs/', '')))
    else:
        sys.stderr.write(colored('Cannot match on log file names for s3 logs when filename is not in s3 pattern', 'red'))
        if args.no_name_fetch_off:
            sys.stderr.write(colored('Will not fetch any s3 logs beacuse --no-name-fetch-off is set, remove this setting to fetch all for this case instead', 'red'))
            return False
        else:
            sys.stderr.write(colored('Will fetch all s3 logs, set --no-name-fetch-off to skip s3 logs instead for this case', 'red'))
            return True
