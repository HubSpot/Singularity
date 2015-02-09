import sys
import logfetch_base
import requests
import time
import threading
from termcolor import colored

TAIL_LOG_FORMAT = '{0}/sandbox/{1}/read'
READ_INTERVAL = 5
THREAD_TIMEOUT = 100000

def start_tail(args):
  if args.requestId:
    sys.stderr.write('Fetching tasks\n')
    tasks = [str(t) for t in logfetch_base.tasks_for_requests(args)]
  else:
    tasks = [args.taskId]
  if args.verbose:
    sys.stderr.write(colored('Tailing logs for tasks:\n', 'green'))
    for t in tasks:
      sys.stderr.write(colored('{0}\n'.format(t), 'yellow'))
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
    sys.stderr.write(colored('Stopping tail', 'magenta'))
    sys.exit(0)

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
      offset = self.get_initial_offset(uri, path)
    except ValueError:
      sys.stderr.write(colored('Could not tail logs for task {0}, check that the task is still active and that the slave it runs on has not been decommissioned\n'.format(task), 'red'))
      keep_trying = False
    while keep_trying:
      try:
        offset = self.fetch_new_log_data(uri, path, offset, args, task)
        time.sleep(5)
      except ValueError:
        sys.stderr.write(colored('Could not tail logs for task {0}, check that the task is still active and that the slave it runs on has not been decommissioned\n'.format(task), 'red'))
        keep_trying = False

  def get_initial_offset(self, uri, path):
    params = {"path" : path}
    return requests.get(uri, params=params).json()['offset']

  def fetch_new_log_data(self, uri, path, offset, args, task):
    params = {
      "path" : path,
      "offset" : offset
    }
    if args.grep:
      params['grep'] = args.grep
    response = requests.get(uri, params=params).json()
    prefix = '({0}) =>\n'.format(task) if args.verbose else ''
    if response['data'] != '':
        sys.stdout.write('{0}{1}'.format(colored(prefix, 'cyan'), response['data']))
    return offset + len(response['data'].encode('utf-8'))
