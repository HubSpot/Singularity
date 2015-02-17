import sys
import requests
from termcolor import colored

ERROR_STATUS_FORMAT = 'Singularity responded with an invalid status code ({0})'

def get_json_response(uri, params={}):
  singularity_response = requests.get(uri, params=params)
  if singularity_response.status_code < 199 or singularity_response.status_code > 299:
    sys.stderr.write(uri + '\n')
    sys.stderr.write(str(params) + '\n')
    sys.stderr.write(colored(ERROR_STATUS_FORMAT.format(singularity_response.status_code), 'red') + '\n')
  return singularity_response.json()

