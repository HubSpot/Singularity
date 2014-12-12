import os
import sys
import grequests
from glob import glob
from termcolor import colored

from callbacks import generate_callback
from singularity_request import get_json_response
import logfetch_base

DOWNLOAD_FILE_FORMAT = '{0}/sandbox/{1}/download'
BROWSE_FOLDER_FORMAT = '{0}/sandbox/{1}/browse'
REQUEST_TASKS_FORMAT = '/history/request/{0}/tasks'

def download_live_logs(args):
  tasks = tasks_to_check(args)
  async_requests = []
  zipped_files = []
  all_logs = []
  sys.stderr.write(colored('Removing old service.log files', 'blue') + '\n')
  for f in glob('{0}/*service.log'.format(args.dest)):
    os.remove(f)
  sys.stderr.write(colored('Downloading current live log files', 'blue') + '\n')
  for task in tasks:
    uri = DOWNLOAD_FILE_FORMAT.format(logfetch_base.base_uri(args), task)
    service_log = '{0}-service.log'.format(task)
    tail_log = '{0}-tail_of_finished_service.log'.format(task)
    async_requests.append(
      grequests.AsyncRequest('GET',uri ,
        callback=generate_callback(uri, args.dest, service_log, args.chunk_size),
        params={'path' : '{0}/service.log'.format(task)}
      )
    )
    all_logs.append('{0}/{1}'.format(args.dest, service_log))
    async_requests.append(
      grequests.AsyncRequest('GET',uri ,
        callback=generate_callback(uri, args.dest, tail_log, args.chunk_size),
        params={'path' : '{0}/tail_of_finished_service.log'.format(task)}
      )
    )
    all_logs.append('{0}/{1}'.format(args.dest, service_log))
    for log_file in logs_folder_files(args, task):
      logfile_name = '{0}-{1}'.format(task, log_file)
      async_requests.append(
        grequests.AsyncRequest('GET',uri ,
          callback=generate_callback(uri, args.dest, logfile_name, args.chunk_size),
          params={'path' : '{0}/logs/{1}'.format(task, log_file)}
        )
      )
      if logfile_name.endswith('.gz'):
        zipped_files.append('{0}/{1}'.format(args.dest, logfile_name))
      all_logs.append('{0}/{1}'.format(args.dest, logfile_name.replace('.gz', '.log')))

  grequests.map(async_requests, stream=True, size=args.num_parallel_fetches)
  logfetch_base.unpack_logs(zipped_files)
  return all_logs

def tasks_to_check(args):
  if args.taskId:
    return [args.taskId]
  else:
    return tasks_for_request(args)

def tasks_for_request(args):
  if args.requestId and args.deployId:
      tasks = [task["taskId"]["id"] for task in all_tasks_for_request(args) if task["taskId"]["deployId"] == args.deployId]
  else:
    tasks = [task["taskId"]["id"] for task in all_tasks_for_request(args)[0:args.task_count]]
  return tasks

def all_tasks_for_request(args):
  uri = '{0}{1}'.format(logfetch_base.base_uri(args), REQUEST_TASKS_FORMAT.format(args.requestId))
  return get_json_response(uri)

def logs_folder_files(args, task):
  uri = BROWSE_FOLDER_FORMAT.format(logfetch_base.base_uri(args), task)
  files_json = get_json_response(uri, {'path' : '{0}/logs'.format(task)})
  if 'files' in files_json:
    files = files_json['files']
    return [f['name'] for f in files]
  else:
    return [f['path'].rsplit('/')[-1] for f in files_json]
