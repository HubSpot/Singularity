import requests

ERROR_STATUS_FORMAT = 'Singularity responded with an invalid status code ({0})'

def get_json_response(uri, params={}):
  singularity_response = requests.get(uri, params=params)
  if singularity_response.status_code < 199 or singularity_response.status_code > 299:
    print uri
    print params
    exit(ERROR_STATUS_FORMAT.format(singularity_response.status_code))
  return singularity_response.json()

