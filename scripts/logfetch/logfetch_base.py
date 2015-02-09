import os
import sys
import gzip
import fnmatch
from datetime import datetime
from termcolor import colored
from singularity_request import get_json_response

BASE_URI_FORMAT = '{0}{1}'
ALL_REQUESTS = '/requests'
REQUEST_TASKS_FORMAT = '/history/request/{0}/tasks'
ACTIVE_TASKS_FORMAT = '/history/request/{0}/tasks/active'

def unpack_logs(logs):
  for zipped_file in logs:
    try:
      if os.path.isfile(zipped_file):
        file_in = gzip.open(zipped_file, 'rb')
        unzipped = zipped_file.replace('.gz', '.log')
        file_out = open(unzipped, 'wb')
        file_out.write(file_in.read())
        file_out.close()
        file_in.close
        os.remove(zipped_file)
        sys.stderr.write(colored('Unpacked ', 'green') + colored(zipped_file, 'white') + '\n')
    except:
      if os.path.isfile(zipped_file):
        os.remove(zipped_file)
      sys.stderr.write(colored('Could not unpack {0}'.format(zipped_file), 'red') + '\n')
      continue

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
  return all_tasks

def log_matches(inputString, pattern):
  return fnmatch.fnmatch(inputString, pattern) or fnmatch.fnmatch(inputString, pattern + '*.gz')

def all_tasks_for_request(args, request):
  uri = '{0}{1}'.format(base_uri(args), ACTIVE_TASKS_FORMAT.format(request))
  active_tasks = get_json_response(uri)
  if hasattr(args, 'start_days'):
    uri = '{0}{1}'.format(base_uri(args), REQUEST_TASKS_FORMAT.format(request))
    historical_tasks = get_json_response(uri)
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
  requests = get_json_response(uri)
  included_requests = []
  for request in requests:
    if fnmatch.fnmatch(request['request']['id'], args.requestId):
      included_requests.append(request['request']['id'])
  return included_requests

def is_in_date_range(args, timestamp):
  timedelta = datetime.utcnow() - datetime.utcfromtimestamp(timestamp)
  if args.end_days:
    return False if timedelta.days > args.start_days or timedelta.days <= args.end_days else True
  else:
    return False if timedelta.days > args.start_days else True
