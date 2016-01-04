import os
import sys
import gzip
import fnmatch
from datetime import datetime, timedelta
from termcolor import colored
from singularity_request import get_json_response

BASE_URI_FORMAT = '{0}{1}'
ALL_REQUESTS = '/requests'
REQUEST_TASKS_FORMAT = '/history/request/{0}/tasks'
ACTIVE_TASKS_FORMAT = '/history/request/{0}/tasks/active'

def unpack_logs(args, logs):
  successful = []
  goal = len(logs)
  progress = 0
  for zipped_file in logs:
    try:
      if os.path.isfile(zipped_file):
        if args.verbose and not args.silent:
          sys.stderr.write(colored('Starting unpack of {0}'.format(zipped_file), 'magenta') + '\n')
        file_in = gzip.open(zipped_file, 'rb')
        unzipped = zipped_file.replace('.gz', '.log')
        file_out = open(unzipped, 'wb')
        file_out.write(file_in.read())
        file_out.close()
        file_in.close
        os.remove(zipped_file)
        progress += 1
        if args.verbose and not args.silent:
          sys.stderr.write(colored('Unpacked log {0}/{1}'.format(progress, goal), 'green') + colored(zipped_file, 'white') + '\n')
        else:
          update_progress_bar(progress, goal, 'Unpack', args.silent)
        successful.append(unzipped)
      else:
        progress += 1
        update_progress_bar(progress, goal, 'Unpack', args.silent)
    except Exception as e:
      print e
      if os.path.isfile(zipped_file):
        os.remove(zipped_file)
      sys.stderr.write(colored('Could not unpack {0}'.format(zipped_file), 'red') + '\n')
      continue
  if not args.silent:
    sys.stderr.write('\n')
  return successful

def base_uri(args):
  if not args.singularity_uri_base:
    exit("Specify a base uri for Singularity (-u)")
  uri_prefix = "" if args.singularity_uri_base.startswith(("http://", "https://")) else "http://"
  return BASE_URI_FORMAT.format(uri_prefix, args.singularity_uri_base)

def tasks_for_requests(args):
  all_tasks = []
  for request in all_requests(args):
    if args.requestId and args.deployId:
        tasks = [task["taskId"]["id"] for task in all_tasks_for_request(args, request) if log_matches(task["taskId"]["deployId"], args.deployId)]
    else:
        tasks = [task["taskId"]["id"] for task in all_tasks_for_request(args, request)]
        tasks = tasks[0:args.task_count] if hasattr(args, 'task_count') else tasks
    all_tasks = all_tasks + tasks
  if not all_tasks:
    sys.stderr.write(colored('No tasks found, check that the request/task you are searching for exists...', 'red'))
    exit(1)
  return all_tasks

def log_matches(inputString, pattern):
  return fnmatch.fnmatch(inputString, pattern) or fnmatch.fnmatch(inputString, pattern + '*.gz')

def all_tasks_for_request(args, request):
  uri = '{0}{1}'.format(base_uri(args), ACTIVE_TASKS_FORMAT.format(request))
  active_tasks = get_json_response(uri, args)
  if hasattr(args, 'start'):
    uri = '{0}{1}'.format(base_uri(args), REQUEST_TASKS_FORMAT.format(request))
    historical_tasks = get_json_response(uri, args)
    if len(historical_tasks) == 0:
      return active_tasks
    elif len(active_tasks) == 0:
      return historical_tasks
    else:
      return active_tasks + [h for h in historical_tasks if is_in_date_range(args, int(str(h['updatedAt'])[0:-3]))]
  else:
    return active_tasks

def all_requests(args):
  uri = '{0}{1}'.format(base_uri(args),  ALL_REQUESTS)
  requests = get_json_response(uri, args)
  included_requests = []
  for request in requests:
    if fnmatch.fnmatch(request['request']['id'], args.requestId):
      included_requests.append(request['request']['id'])
  return included_requests

def is_in_date_range(args, timestamp):
  timstamp_datetime = datetime.utcfromtimestamp(timestamp)
  if args.end:
    return False if (timstamp_datetime < args.start or timstamp_datetime > args.end) else True
  else:
    return False if timedelta.days < args.start else True

def update_progress_bar(progress, goal, progress_type, silent):
  bar_length = 30
  percent = float(progress) / goal
  hashes = '#' * int(round(percent * bar_length))
  spaces = ' ' * (bar_length - len(hashes))
  if percent > .8:
    color = 'green'
  elif percent > .5:
    color = 'cyan'
  elif percent > .25:
    color = 'yellow'
  else:
    color = 'blue'
  if not silent:
    sys.stderr.write("\r{0} Progress: [".format(progress_type) + colored("{0}".format(hashes + spaces), color) + "] {0}%".format(int(round(percent * 100))))
    sys.stderr.flush()
