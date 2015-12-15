import os
import sys
import logfetch_base
import requests
import time
import fnmatch
import threading
from grep import grep_command
from termcolor import colored
from singularity_request import get_json_response

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
  if args.verbose:
    sys.stderr.write(colored('Tailing logs for tasks:\n', 'green'))
    if not args.silent:
      for t in tasks:
        sys.stderr.write(colored('{0}\n'.format(t), 'yellow'))
  if not args.silent:
    sys.stderr.write(colored('ctrl+c to exit\n', 'cyan'))
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
    if not args.silent:
      sys.stderr.write(colored('Stopping tail', 'magenta') + '\n')
    sys.exit(0)

def logs_folder_files(args, task):
  uri = BROWSE_FOLDER_FORMAT.format(logfetch_base.base_uri(args), task)
  files_json = get_json_response(uri, args, {'path' : '{0}/logs'.format(task)})
  if 'files' in files_json:
    files = files_json['files']
    return [f['name'] for f in files if valid_logfile(f)]
  else:
    return [f['path'].rsplit('/')[-1] for f in files_json if valid_logfile(f)]

def base_directory_files(args, task):
  uri = BROWSE_FOLDER_FORMAT.format(logfetch_base.base_uri(args), task)
  files_json = get_json_response(uri, args)
  if 'files' in files_json:
    files = files_json['files']
    return [f['name'] for f in files if valid_logfile(f)]
  else:
    return [f['path'].rsplit('/')[-1] for f in files_json if valid_logfile(f)]

def valid_logfile(fileData):
  not_a_directory = not fileData['mode'].startswith('d')
  is_a_logfile = fnmatch.fnmatch(fileData['name'], '*.log') or fnmatch.fnmatch(fileData['name'], '*.out') or fnmatch.fnmatch(fileData['name'], '*.err')
  return not_a_directory and is_a_logfile

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

  def fetch_new_log_data(self, uri, path, offset, args, task):
    params = {
      "path" : path,
      "offset" : offset
    }
    response = requests.get(uri, params=params, headers=args.headers).json()
    prefix = '({0}) =>\n'.format(task) if args.verbose else ''
    if len(response['data'].encode('utf-8')) > 0:
      if args.grep:
        filename = '{0}/.grep{1}'.format(args.dest, self.Task)
        self.create_grep_file(args, filename, response['data'])
        output = os.popen(grep_command(args, filename)).read()
        sys.stdout.write('{0}{1}'.format(colored(prefix, 'cyan'), output))
        self.remove_grep_file(filename)
      else:
        sys.stdout.write('{0}{1}'.format(colored(prefix, 'cyan'), response['data'].encode('utf-8')))
      return offset + len(response['data'].encode('utf-8'))
    else:
      return offset

  def create_grep_file(self, args, filename, content):
    grep_file = open(filename, 'wb')
    grep_file.write(content.encode('utf-8'))
    grep_file.close()


  def remove_grep_file(self, grep_file):
    if os.path.isfile(grep_file):
      os.remove(grep_file)

  def show_available_files(self, args, task):
    sys.stderr.write(colored('Available files (-l arguments):\n', 'cyan'))
    try:
      for f in base_directory_files(args, task):
        sys.stderr.write(f + '\n')
      for f in logs_folder_files(args, task):
        sys.stderr.write('logs/' + f + '\n')
    except:
      sys.stderr.write(colored('Could not fetch list of files', 'red'))

