import os
import sys
import gzip
import fnmatch
import socket
from datetime import datetime, timedelta
from termcolor import colored
from singularity_request import get_json_response
from urlparse import urlparse

BASE_URI_FORMAT = '{0}{1}'
ALL_REQUESTS = '/requests'
REQUEST_TASKS_FORMAT = '/history/request/{0}/tasks'
ACTIVE_TASKS_FORMAT = '/history/request/{0}/tasks/active'

CACHED_ADDRS = {}

def unpack_logs(args, logs):
  successful = []
  for zipped_file in logs:
    try:
      if os.path.isfile(zipped_file):
        if args.verbose:
          sys.stderr.write(colored('Starting unpack of {0}'.format(zipped_file), 'magenta') + '\n')
        file_in = gzip.open(zipped_file, 'rb')
        unzipped = zipped_file.replace('.gz', '.log')
        file_out = open(unzipped, 'wb')
        file_out.write(file_in.read())
        file_out.close()
        file_in.close
        os.remove(zipped_file)
        if args.verbose:
          sys.stderr.write(colored('Unpacked ', 'green') + colored(zipped_file, 'white') + '\n')
        else:
          sys.stderr.write(colored('.', 'green'))
        successful.append(unzipped)
    except Exception as e:
      print e
      if os.path.isfile(zipped_file):
        os.remove(zipped_file)
      sys.stderr.write(colored('Could not unpack {0}'.format(zipped_file), 'red') + '\n')
      continue
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

def host_to_ip(address):
  parsed_url = urlparse(address)
  ip = getIP(parsed_url.netloc)
  if ip:
    CACHED_ADDRS[parsed_url.netloc] = ip
    parsed_url = parsed_url._replace(netloc=ip.replace('\'', ''))
  print parsed_url.geturl()
  return parsed_url.geturl()

def getIP(d):
    try:
        if d in CACHED_ADDRS:
          return CACHED_ADDRS[d]
        else:
          data = socket.gethostbyname(d)
          ip = repr(data)
        return ip
    except Exception as e:
        print e
        return False
