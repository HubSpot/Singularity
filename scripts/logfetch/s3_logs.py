import os
import sys
import grequests
import logfetch_base
import time
from termcolor import colored
import callbacks

S3LOGS_URI_FORMAT = '{0}/logs/search'

FILE_REGEX="\d{13}-([^-]*)-\d{8,20}\.gz"

progress = 0
goal = 0

def download_s3_logs(args):
    if not args.silent:
        sys.stderr.write(colored('Checking for S3 log files', 'cyan') + '\n')
    callbacks.progress = 0
    logs = find_all_s3_logs(args)
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
        logfetch_base.log(colored('{0} logs in time range, starting S3 Downloads with {1} parallel fetches\n'.format(len(async_requests), args.num_parallel_fetches), 'cyan'), args, False)
        callbacks.goal = len(async_requests)
        grequests.map(async_requests, stream=True, size=args.num_parallel_fetches)
    else:
        logfetch_base.log(colored('No S3 logs to download\n', 'cyan'), args, False)
    logfetch_base.log(colored('Finished S3 Logs\n', 'cyan'), args, False)
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

def find_all_s3_logs(args):
    start = int(time.mktime(args.start.timetuple()) * 1000)
    end = int(time.mktime(args.end.timetuple()) * 1000)
    tasks = []
    requests_and_deploys = {}

    if args.taskId:
        tasks.append(args.taskId)
    else:
        requests = logfetch_base.all_requests(args)
        tasks.extend(logfetch_base._tasks_for_requests(args, requests))
        for request in requests:
            if args.deployId:
                requests_and_deploys[request] = [args.deployId]
            else:
                requests_and_deploys[request] = []
    search_data = {
        'start': start,
        'end': end,
        'taskIds': tasks,
        'requestsAndDeploys': requests_and_deploys,
        'maxPerPage': args.s3_page_size
    }

    finished = False
    found_logs = []
    while not finished:
        s3_search_result = logfetch_base.get_json_response(s3_logs_uri(args), args, data=search_data)
        found_logs.extend(s3_search_result['results'])
        finished = s3_search_result['lastPage']
        search_data['continuationTokens'] = s3_search_result['continuationTokens']
        if not args.silent:
            sys.stderr.write("\rFound {0} additional logs({1} total)".format(len(s3_search_result['results']), len(found_logs)))
            sys.stderr.flush()
    if not args.silent:
        sys.stderr.write("\n")
    return found_logs

def s3_logs_uri(args):
    return S3LOGS_URI_FORMAT.format(logfetch_base.base_uri(args))

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
