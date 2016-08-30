import errors
import os
import sys
import logfetch_base
import requests
import time
import fnmatch
import threading
from grep import grep_command
from termcolor import colored
from logfetch_base import log, get_json_response

TAIL_LOG_FORMAT = '{0}/sandbox/{1}/read'
READ_INTERVAL = 5
THREAD_TIMEOUT = 100000
BROWSE_FOLDER_FORMAT = '{0}/sandbox/{1}/browse'

def start_tail(args):
    if args.requestId:
        if not args.silent:
            sys.stderr.write('Fetching tasks\n')
        tasks = [str(t) for t in logfetch_base.tasks_for_requests(args)]
    else:
        tasks = [args.taskId]
    log(colored('Tailing logs for tasks:\n', 'green'), args, True)
    for t in tasks:
        log(colored('{0}\n'.format(t), 'yellow'), args, True)
    log(colored('ctrl+c to exit\n', 'cyan'), args, False)
    try:
        threads = []
        for task in tasks:
            thread = LogStreamer(args, task)
            threads.append(thread)
            thread.start()
        for t in threads:
            t.join(THREAD_TIMEOUT) #Need a timeout otherwise can't be killed by ctrl+c
            if not t.isAlive:
                break
    except KeyboardInterrupt:
        log(colored('Stopping tail', 'magenta') + '\n', args, False)
        sys.exit(0)

def logs_folder_files(args, task):
    uri = BROWSE_FOLDER_FORMAT.format(logfetch_base.base_uri(args), task)
    files_json = get_json_response(uri, args, {'path' : '{0}/logs'.format(task)})
    if 'files' in files_json:
        files = files_json['files']
        return [f['name'] for f in files if is_valid_tail_log(f)]
    else:
        return [f['path'].rsplit('/')[-1] for f in files_json if is_valid_tail_log(f)]

def base_directory_files(args, task):
    uri = BROWSE_FOLDER_FORMAT.format(logfetch_base.base_uri(args), task)
    files_json = get_json_response(uri, args)
    if 'files' in files_json:
        files = files_json['files']
        return [f['name'] for f in files if is_valid_tail_log(f)]
    else:
        return [f['path'].rsplit('/')[-1] for f in files_json if is_valid_tail_log(f)]

def is_valid_tail_log(file_data):
    return logfetch_base.is_valid_log(file_data)

class LogStreamer(threading.Thread):
    def __init__(self, args, task):
        threading.Thread.__init__(self)
        self.daemon = True
        self.Args = args
        self.Task = task

    def run(self):
        self.stream_log_for_task(self.Args, self.Task)

    def stream_log_for_task(self, args, task):
        uri = TAIL_LOG_FORMAT.format(logfetch_base.base_uri(args), task)
        path = '{0}/{1}'.format(task, args.logfile)
        keep_trying = True
        try:
            params = {"path" : path}
            logfile_response = requests.get(uri, params=params, headers=args.headers)
            logfile_response.raise_for_status()
            offset = long(logfile_response.json()['offset'])
        except ValueError:
            sys.stderr.write(colored('Could not get initial offset for log in task {0}, check that the task is still active and that the slave it runs on has not been decommissioned\n'.format(task), 'red'))
            keep_trying = False
        except:
            sys.stderr.write(colored('Could not find log file at path {0} for task {1}, check your -l arg and try again\n'.format(args.logfile, task), 'red'))
            self.show_available_files(args, task)
            keep_trying = False
        while keep_trying:
            try:
                offset = self.fetch_new_log_data(uri, path, offset, args, task)
                time.sleep(5)
            except ValueError:
                sys.stderr.write(colored('Could not tail logs for task {0}, check that the task is still active and that the slave it runs on has not been decommissioned\n'.format(task), 'red'))
                keep_trying = False
            except errors.NoTailDataError:
                sys.stderr.write(colored('Could not tail logs for task {0}, response had no data and was not a 2xx\n'.format(task), 'red'))
                sys.stderr.flush()
                keep_trying = False

    def fetch_new_log_data(self, uri, path, offset, args, task):
        params = {
            "path" : path,
            "offset" : offset
        }
        response_obj = requests.get(uri, params=params, headers=args.headers)
        response = response_obj.json()
        if 'data' not in response:
            if response_obj.status_code < 199 or response_obj.status_code > 299:
                raise errors.NoTailDataError()
            else:
                sys.stderr.write(colored('Log tail data missing, retrying...\n'.format(task), 'red'))
                sys.stderr.flush()
                return offset

        prefix = '({0}) =>\n'.format(task) if args.verbose else ''
        if len(response['data'].encode('utf-8')) > 0:
            sys.stdout.write('{0}{1}'.format(colored(prefix, 'cyan'), response['data'].encode('utf-8')))
            sys.stdout.flush()
            return offset + len(response['data'].encode('utf-8'))
        else:
            return offset

    def show_available_files(self, args, task):
        sys.stderr.write(colored('Available files (-l arguments):\n', 'cyan'))
        try:
            for f in base_directory_files(args, task):
                sys.stderr.write(f + '\n')
            for f in logs_folder_files(args, task):
                sys.stderr.write('logs/' + f + '\n')
        except:
            sys.stderr.write(colored('Could not fetch list of files', 'red'))

