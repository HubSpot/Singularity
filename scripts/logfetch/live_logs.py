import os
import fnmatch
import grequests
import callbacks
import logfetch_base
from termcolor import colored

DOWNLOAD_FILE_FORMAT = 'http://{0}:5051/files/download'
BROWSE_FOLDER_FORMAT = '{0}/sandbox/{1}/browse'
TASK_HISTORY_FORMAT = '{0}/history/task/{1}'

def download_live_logs(args):
    logfetch_base.log(colored('Finding current live log files', 'cyan') + '\n', args, False)
    tasks = tasks_to_check(args)
    async_requests = []
    all_logs = []
    callbacks.progress = 0
    tasks_check_progress = 0
    tasks_check_goal = len(tasks)
    for task in tasks:
        metadata = files_json(args, task)
        if 'slaveHostname' in metadata:
            uri = DOWNLOAD_FILE_FORMAT.format(metadata['slaveHostname'])
            for log_file in base_directory_files(args, task, metadata):
                logfile_name = '{0}-{1}'.format(task, log_file)
                if not args.logtype or (args.logtype and logfetch_base.log_matches(log_file, args.logtype.replace('logs/', ''))):
                    if should_download(args, logfile_name, task):
                        async_requests.append(
                            grequests.AsyncRequest('GET',uri ,
                                callback=callbacks.generate_callback(uri, args.dest, logfile_name, args.chunk_size, args.verbose, args.silent),
                                params={'path' : '{0}/{1}/{2}'.format(metadata['fullPathToRoot'], metadata['currentDirectory'], log_file)},
                                headers=args.headers
                            )
                        )
                    all_logs.append('{0}/{1}'.format(args.dest, logfile_name))
                elif args.logtype:
                    logfetch_base.log(colored('Excluding log {0}, doesn\'t match {1}'.format(log_file, args.logtype), 'magenta') + '\n', args, True)
            for log_file in logs_folder_files(args, task):
                logfile_name = '{0}-{1}'.format(task, log_file)
                if not args.logtype or (args.logtype and logfetch_base.log_matches(log_file, args.logtype.replace('logs/', ''))):
                    if should_download(args, logfile_name, task):
                        async_requests.append(
                            grequests.AsyncRequest('GET',uri ,
                                callback=callbacks.generate_callback(uri, args.dest, logfile_name, args.chunk_size, args.verbose, args.silent),
                                params={'path' : '{0}/{1}/logs/{2}'.format(metadata['fullPathToRoot'], metadata['currentDirectory'], log_file)},
                                headers=args.headers
                            )
                        )
                    all_logs.append('{0}/{1}'.format(args.dest, logfile_name))
                elif args.logtype:
                    logfetch_base.log(colored('Excluding log {0}, doesn\'t match {1}'.format(log_file, args.logtype), 'magenta') + '\n', args, True)
        tasks_check_progress += 1
        logfetch_base.update_progress_bar(tasks_check_progress, tasks_check_goal, 'Log Finder', args.silent)

    if async_requests:
        logfetch_base.log(colored('\nStarting {0} live logs downloads\n'.format(len(async_requests)), 'cyan'), args, False)
        callbacks.goal = len(async_requests)
        grequests.map(async_requests, stream=True, size=args.num_parallel_fetches)
    return all_logs

def tasks_to_check(args):
    if args.taskId:
        return [args.taskId]
    else:
        return logfetch_base.tasks_for_requests(args)

def task_history(args, task):
    uri = TASK_HISTORY_FORMAT.format(logfetch_base.base_uri(args), task)
    return logfetch_base.get_json_response(uri, args)

def task_still_running(args, task, history):
    try:
        last_state = history['taskUpdates'][-1]['taskState']
        return last_state in ['TASK_RUNNING', 'TASK_STARTING', 'TASK_LAUNCHED', 'TASK_CLEANING']
    except:
        return True

def files_json(args, task):
    uri = BROWSE_FOLDER_FORMAT.format(logfetch_base.base_uri(args), task)
    return logfetch_base.get_json_response(uri, args, {}, True)

def logs_folder_files(args, task):
    uri = BROWSE_FOLDER_FORMAT.format(logfetch_base.base_uri(args), task)
    files_json = logfetch_base.get_json_response(uri, args, {'path' : '{0}/logs'.format(task)}, True)
    if 'files' in files_json:
        files = files_json['files']
        return [f['name'] for f in files if logfetch_base.is_in_date_range(args, f['mtime'])]
    else:
        return [f['path'].rsplit('/')[-1] for f in files_json if logfetch_base.is_in_date_range(args, f['mtime'])]

def base_directory_files(args, task, files_json):
    if 'files' in files_json:
        files = files_json['files']
        return [f['name'] for f in files if valid_logfile(args, f)]
    else:
        return [f['path'].rsplit('/')[-1] for f in files_json if valid_logfile(args, f)]

def valid_logfile(args, fileData):
        is_in_range = logfetch_base.is_in_date_range(args, fileData['mtime'])
        not_a_directory = not fileData['mode'].startswith('d')
        is_a_logfile = fnmatch.fnmatch(fileData['name'], '*.log') or fnmatch.fnmatch(fileData['name'], '*.out') or fnmatch.fnmatch(fileData['name'], '*.err')
        return is_in_range and not_a_directory and is_a_logfile

def should_download(args, filename, task):
    if args.use_cache and already_downloaded(args, filename):
        logfetch_base.log(colored('Using cached version of file {0}\n'.format(filename), 'magenta'), args, True)
        return False
    if filename.endswith('.gz') and already_downloaded(args, filename):
        logfetch_base.log(colored('Using cached version of file {0}, zipped file has not changed\n'.format(filename), 'magenta'), args, True)
        return False
    history = task_history(args, task)
    if not task_still_running(args, task, history) and already_downloaded(args, filename) and file_not_too_old(args, history, filename):
        logfetch_base.log(colored('Using cached version of file {0}, {1}, file has not changed\n'.format(filename, history['taskUpdates'][-1]['taskState']), 'magenta'), args, True)
    else:
        logfetch_base.log(colored('Will download file {0}, version on the server is newer than cached version\n'.format(filename), 'magenta'), args, True)

    return True

def file_not_too_old(args, history, filename):
    state_updated_at = int(str(history['taskUpdates'][-1]['timestamp'])[0:-3])
    return int(os.path.getmtime('{0}/{1}'.format(args.dest, filename))) > state_updated_at

def already_downloaded(args, filename):
    have_file = (os.path.isfile('{0}/{1}'.format(args.dest, filename.replace('.gz', '.log'))) or os.path.isfile('{0}/{1}'.format(args.dest, filename)))
    return have_file
