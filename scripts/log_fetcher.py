import argparse
import ConfigParser
import sys
import os
import grequests
import requests
import gzip
from glob import glob
from termcolor import colored

TASK_FORMAT = '/task/{0}'
DEPLOY_FORMAT = '/request/{0}/deploy/{1}'
REQUEST_FORMAT = '/request/{0}'
REQUEST_TASKS_FORMAT = '/history' + REQUEST_FORMAT + '/tasks'

S3LOGS_URI_FORMAT = '{0}/logs{1}'
CALLBACK_FORMAT = '{0}/{1}'
BASE_URI_FORMAT = '{0}{1}'

BROWSE_FOLDER_FORMAT = '{0}/sandbox/{1}/browse'
DOWNLOAD_FILE_FORMAT = '{0}/sandbox/{1}/download'

ERROR_STATUS_FORMAT = 'Singularity responded with an invalid status code ({0})'
CONF_READ_ERR_FORMAT = 'Could not load config from {0} due to {1}'

GREP_COMMAND_FORMAT = 'xargs -n {0} grep \'{1}\' < {2}'

DEFAULT_CONF_FILE = os.path.expanduser('~/.logfetch')

class FakeSectionHead(object):
  def __init__(self, fp):
    self.fp = fp
    self.sechead = '[Defaults]\n'

  def readline(self):
    if self.sechead:
      try: return self.sechead
      finally: self.sechead = None
    else: return self.fp.readline()

def exit(reason):
  print colored(reason, 'red')
  sys.exit(1)

def main(parser, args):
  all_logs = []
  all_logs += download_s3_logs(args)
  all_logs += download_live_logs(args)
  grep_files(args, all_logs)

def grep_files(args, all_logs):
  if args.grep:
    greplist_filename = '{0}/.greplist'.format(args.dest)
    create_greplist(args, all_logs, greplist_filename)
    grep_command = GREP_COMMAND_FORMAT.format(len(all_logs), args.grep, greplist_filename)
    print colored('Running "{0}" this might take a minute'.format(grep_command), 'blue')
    print os.popen(grep_command).read()
    remove_greplist(greplist_filename)
    print colored('Finished grep, exiting', 'green')

def create_greplist(args, all_logs, greplist_filename):
  greplist_file = open(greplist_filename, 'wb')
  for log in all_logs:
    greplist_file.write('{0}\n'.format(log))
  greplist_file.close()

def remove_greplist(greplist_filename):
  if os.path.isfile(greplist_filename):
    os.remove(greplist_filename)

def download_live_logs(args):
  tasks = tasks_to_check(args)
  async_requests = []
  zipped_files = []
  all_logs = []
  print colored('Removing old service.log files', 'blue')
  for f in glob('{0}/*service.log'.format(args.dest)):
    os.remove(f)
  print colored('Downloading current live log files', 'blue')
  for task in tasks:
    uri = DOWNLOAD_FILE_FORMAT.format(base_uri(args), task)
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
  unpack_logs(zipped_files)
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
  uri = '{0}{1}'.format(base_uri(args), REQUEST_TASKS_FORMAT.format(args.requestId))
  return get_json_response(uri)

def logs_folder_files(args, task):
  uri = BROWSE_FOLDER_FORMAT.format(base_uri(args), task)
  files = get_json_response(uri, {'path' : '{0}/logs'.format(task)})
  return [f['path'].rsplit('/')[-1] for f in files]


def download_s3_logs(args):
  print colored('Checking for S3 log files', 'blue')
  logs = get_json_response(singularity_s3logs_uri(args))
  async_requests = []
  all_logs = []
  for log_file in logs:
    filename = log_file['key'][log_file['key'].rfind('/') + 1:]
    all_logs.append('{0}/{1}'.format(args.dest, filename.replace('.gz', '.log')))
    if not (os.path.isfile('{0}/{1}'.format(args.dest, filename)) or os.path.isfile('{0}/{1}'.format(args.dest, filename.replace('.gz', '.log')))):
        async_requests.append(
            grequests.AsyncRequest('GET', log_file['getUrl'],
                callback=generate_callback(log_file['getUrl'], args.dest, filename, args.chunk_size)
            )
        )
  grequests.map(async_requests, stream=True, size=args.num_parallel_fetches)
  zipped_files = ['{0}/{1}'.format(args.dest, log_file['key'][log_file['key'].rfind('/') + 1:]) for log_file in logs]
  unpack_logs(zipped_files)
  print colored('All S3 logs up to date', 'blue')
  return all_logs

def unpack_logs(logs):
  for zipped_file in logs:
    if os.path.isfile(zipped_file):
      file_in = gzip.open(zipped_file, 'rb')
      unzipped = zipped_file.replace('.gz', '.log')
      file_out = open(unzipped, 'wb')
      file_out.write(file_in.read())
      file_out.close()
      file_in.close
      os.remove(zipped_file)
      print colored('Unpacked {0}'.format(zipped_file), 'green')

def base_uri(args):
  if not args.singularity_uri_base:
    exit("Specify a base uri for Singularity")
  uri_prefix = "" if args.singularity_uri_base.startswith(("http://", "https://")) else "http://"
  uri = BASE_URI_FORMAT.format(uri_prefix, args.singularity_uri_base)
  return uri

def singularity_s3logs_uri(args):
  if args.taskId:
    singularity_path = TASK_FORMAT.format(args.taskId)
  elif args.deployId and args.requestId:
    singularity_path = DEPLOY_FORMAT.format(args.requestId, args.deployId)
  elif args.requestId:
    singularity_path = REQUEST_FORMAT.format(args.requestId)
  else:
    exit("Specify one of taskId, requestId and deployId, or requestId")
  singularity_uri = S3LOGS_URI_FORMAT.format(base_uri(args), singularity_path)

  return singularity_uri

def generate_callback(request, destination, filename, chunk_size):
  path = CALLBACK_FORMAT.format(destination, filename) if destination else filename

  def callback(response, **kwargs):
    with open(path, 'wb') as f:
      for chunk in response.iter_content(chunk_size):
        f.write(chunk)
    print colored('finished downloading {0}'.format(path), 'green')

  return callback

def get_json_response(uri, params={}):
  singularity_response = requests.get(uri, params=params)
  if singularity_response.status_code < 199 or singularity_response.status_code > 299:
    print uri
    print params
    exit(ERROR_STATUS_FORMAT.format(singularity_response.status_code))
  return singularity_response.json()

if __name__ == "__main__":
  conf_parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter, add_help=False)
  conf_parser.add_argument("-c", "--conf_file", help="Specify config file", metavar="FILE")
  args, remaining_argv = conf_parser.parse_known_args()
  conf_file = args.conf_file if args.conf_file else DEFAULT_CONF_FILE
  config = ConfigParser.SafeConfigParser()

  defaults = { "num_parallel_fetches" : 5, "chunk_size" : 8192, "dest" : os.getcwd(), "task_count" : 1 }

  try:
    config.readfp(FakeSectionHead(open(conf_file)))
    defaults.update(dict(config.items("Defaults")))
  except Exception, err:
    print CONF_READ_ERR_FORMAT.format(conf_file, err)

  parser = argparse.ArgumentParser(parents=[conf_parser], description="Fetch log files from Singularity. One can specify either a TaskId, RequestId and DeployId, or RequestId",
          prog="log_fetcher")

  parser.set_defaults(**defaults)
  parser.add_argument("-t", "--taskId", help="TaskId of task to fetch logs for", metavar="taskId")
  parser.add_argument("-r", "--requestId", help="RequestId of request to fetch logs for", metavar="requestId")
  parser.add_argument("--task-count", help="Number of recent tasks per request to fetch logs from", metavar="taskCount")
  parser.add_argument("-d", "--deployId", help="DeployId of task to fetch logs for", metavar="deployId")
  parser.add_argument("--dest", help="Destination directory", metavar="DIR")
  parser.add_argument("-n", "--num-parallel-fetches", help="Number of fetches to make at once", type=int, metavar="INT")
  parser.add_argument("-cs", "--chunk-size", help="Chunk size for writing from response to filesystem", type=int, metavar="INT")
  parser.add_argument("-s", "--singularity-uri-base", help="The base for singularity (eg. http://localhost:8080/singularity/v1)", metavar="URI")
  parser.add_argument("-g", "--grep", help="Regex to grep for (normal grep syntax)", metavar='grep')

  args = parser.parse_args(remaining_argv)

  if args.deployId and not args.requestId:
    exit("Must specify requestId (-r) when specifying deployId")
  elif not args.requestId and not args.deployId and not args.taskId:
    exit('Must specify one of\n - taskId\n - requestId and deployId\n - requestId')

  main(parser, args)
