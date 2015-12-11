import os
import sys
import fnmatch
import grequests
from termcolor import colored
import callbacks
from singularity_request import get_json_response
import logfetch_base

DOWNLOAD_FILE_FORMAT = 'http://{0}:5051/files/download.json'
BROWSE_FOLDER_FORMAT = '{0}/sandbox/{1}/browse'
TASK_HISTORY_FORMAT = '{0}/history/task/{1}'

def download_live_logs(args):
  if not args.silent:
    sys.stderr.write(colored('Finding current live log files', 'cyan') + '\n')
  tasks = tasks_to_check(args)
  async_requests = []
  zipped_files = []
  all_logs = []
  callbacks.progress = 0
  tasks_check_progress = 0
  tasks_check_goal = len(tasks)
  for task in tasks:
    metadata = files_json(args, task)
    if 'slaveHostname' in metadata:
      uri = DOWNLOAD_FILE_FORMAT.format(metadata['slaveHostname'])
      if args.verbose and not args.silent:
        sys.stderr.write(colored('Finding logs in base directory on {0}'.format(metadata['slaveHostname']), 'magenta') + '\n')
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
          if logfile_name.endswith('.gz'):
            zipped_files.append('{0}/{1}'.format(args.dest, logfile_name))
          else:
            all_logs.append('{0}/{1}'.format(args.dest, logfile_name.replace('.gz', '.log')))
        elif args.logtype and args.verbose and not args.silent:
          sys.stderr.write(colored('Excluding log {0}, doesn\'t match {1}'.format(log_file, args.logtype), 'magenta') + '\n')

      if args.verbose and not args.silent:
        sys.stderr.write(colored('Finding logs in logs directory on {0}'.format(metadata['slaveHostname']), 'magenta') + '\n')
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
          if logfile_name.endswith('.gz'):
            zipped_files.append('{0}/{1}'.format(args.dest, logfile_name))
          else:
            all_logs.append('{0}/{1}'.format(args.dest, logfile_name.replace('.gz', '.log')))
        elif args.logtype and args.verbose and not args.silent:
          sys.stderr.write(colored('Excluding log {0}, doesn\'t match {1}'.format(log_file, args.logtype), 'magenta') + '\n')
    tasks_check_progress += 1
    logfetch_base.update_progress_bar(tasks_check_progress, tasks_check_goal, 'Log Finder', args.silent)

  if async_requests:
    if not args.silent:
      sys.stderr.write(colored('\nStarting {0} live logs downloads\n'.format(len(async_requests)), 'cyan'))
    callbacks.goal = len(async_requests)
    grequests.map(async_requests, stream=True, size=args.num_parallel_fetches)
  if zipped_files:
    if not args.silent:
      sys.stderr.write(colored('\nUnpacking {0} log(s)\n'.format(len(zipped_files)), 'cyan'))
    all_logs = all_logs + logfetch_base.unpack_logs(args, zipped_files)
  return all_logs

def tasks_to_check(args):
  if args.taskId:
    return [args.taskId]
  else:
    return logfetch_base.tasks_for_requests(args)

def task_history(args, task):
  uri = TASK_HISTORY_FORMAT.format(logfetch_base.base_uri(args), task)
  return get_json_response(uri, args)

def task_still_running(args, task, history):
  try:
    last_state = history['taskUpdates'][-1]['taskState']
    return last_state in ['TASK_RUNNING', 'TASK_STARTING', 'TASK_LAUNCHED', 'TASK_CLEANING']
  except:
    return True

def files_json(args, task):
  uri = BROWSE_FOLDER_FORMAT.format(logfetch_base.base_uri(args), task)
  return get_json_response(uri, args)

def logs_folder_files(args, task):
  uri = BROWSE_FOLDER_FORMAT.format(logfetch_base.base_uri(args), task)
  files_json = get_json_response(uri, args, {'path' : '{0}/logs'.format(task)})
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
    if args.verbose and not args.silent:
      sys.stderr.write(colored('Using cached version of file {0}\n'.format(filename), 'magenta'))
    return False
  if filename.endswith('.gz') and already_downloaded(args, filename):
    if args.verbose and not args.silent:
      sys.stderr.write(colored('Using cached version of file {0}, zipped file has not changed\n'.format(filename), 'magenta'))
    return False
  history = task_history(args, task)
  if not task_still_running(args, task, history) and already_downloaded(args, filename) and file_not_too_old(args, history, filename):
    if args.verbose and not args.silent:
      sys.stderr.write(colored('Using cached version of file {0}, {1}, file has not changed\n'.format(filename, history['taskUpdates'][-1]['taskState']), 'magenta'))
  else:
    if args.verbose and not args.silent:
      sys.stderr.write(colored('Will download file {0}, version on the server is newer than cached version\n'.format(filename), 'magenta'))

  return True

def file_not_too_old(args, history, filename):
  state_updated_at = int(str(history['taskUpdates'][-1]['timestamp'])[0:-3])
  return int(os.path.getmtime('{0}/{1}'.format(args.dest, filename))) > state_updated_at

def already_downloaded(args, filename):
  have_file = (os.path.isfile('{0}/{1}'.format(args.dest, filename.replace('.gz', '.log'))) or os.path.isfile('{0}/{1}'.format(args.dest, filename)))
  return have_file
