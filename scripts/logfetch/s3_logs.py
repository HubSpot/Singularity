import os
import grequests
from termcolor import colored

import logfetch_base
from singularity_request import get_json_response
from callbacks import generate_callback

TASK_FORMAT = '/task/{0}'
DEPLOY_FORMAT = '/request/{0}/deploy/{1}'
REQUEST_FORMAT = '/request/{0}'
S3LOGS_URI_FORMAT = '{0}/logs{1}'

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
  logfetch_base.unpack_logs(zipped_files)
  print colored('All S3 logs up to date', 'blue')
  return all_logs


def singularity_s3logs_uri(args):
  if args.taskId:
    singularity_path = TASK_FORMAT.format(args.taskId)
  elif args.deployId and args.requestId:
    singularity_path = DEPLOY_FORMAT.format(args.requestId, args.deployId)
  elif args.requestId:
    singularity_path = REQUEST_FORMAT.format(args.requestId)
  else:
    exit("Specify one of taskId, requestId and deployId, or requestId")
  singularity_uri = S3LOGS_URI_FORMAT.format(logfetch_base.base_uri(args), singularity_path)

  return singularity_uri

