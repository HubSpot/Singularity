import os
import sys
import logfetch_base
import requests
import time
import threading
from singularity_request import get_json_response


TAIL_LOG_FORMAT = '{0}/sandbox/{1}/read'
READ_INTERVAL = 5

def tail_logs(args):
  if args.requestId:
    sys.stderr.write('Fetching tasks\n')
    tasks = [str(t) for t in logfetch_base.tasks_for_request(args)]
  else:
    tasks = [args.taskId]
  sys.stderr.write('Tailing logs for tasks:\n')
  for t in tasks:
    sys.stderr.write('{0}\n'.format(t))
  sys.stderr.write('ctrl+c to exit\n')
  try:
    threads = []
    for task in tasks:
      thread = LogStreamer(args, task)
      threads += [thread]
      thread.start()
    while True: # main thread needs something to do so it doesn't kill the others
      time.sleep(1)
  except KeyboardInterrupt:
    sys.stdout.write('Stopping tail')
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
    path = '{0}/{1}'.format(task, args.tail)
    keep_trying = True
    try:
      offset = self.get_initial_offset(uri, path)
    except ValueError:
      sys.stderr.write('Could not tail logs for task {0}, check that the task is still active and that the slave it runs on has not been decommissioned\n'.format(task))
      keep_trying = False
    while keep_trying:
      try:
        offset = self.fetch_new_log_data(uri, path, offset, args.grep)
        time.sleep(5)
      except ValueError:
        sys.stderr.write('Could not tail logs for task {0}, check that the task is still active and that the slave it runs on has not been decommissioned\n'.format(task))
        keep_trying = False

  def get_initial_offset(self, uri, path):
    params = {"path" : path}
    return requests.get(uri, params=params).json()['offset']

  def fetch_new_log_data(self, uri, path, offset, grep):
    params = {
      "path" : path,
      "offset" : offset
    }
    if grep:
      params['grep'] = grep
    response = requests.get(uri, params=params).json()
    sys.stdout.write(response['data'])
    return offset + len(response['data'].encode('utf-8'))
