import sys
import requests
from termcolor import colored

ERROR_STATUS_FORMAT = 'Singularity responded with an invalid status code ({0})'

def get_json_response(uri, args, params={}):
  singularity_response = requests.get(uri, params=params, headers=args.headers)
  if singularity_response.status_code < 199 or singularity_response.status_code > 299:
    if not args.silent:
      sys.stderr.write('{0} params:{1}\n'.format(uri, str(params)))
    sys.stderr.write(colored(ERROR_STATUS_FORMAT.format(singularity_response.status_code), 'red') + '\n')
    if not args.silent:
      sys.stderr.write(colored(singularity_response.text, 'red') + '\n')
    return {}
  return singularity_response.json()

