import os
import sys
import gzip
from termcolor import colored

BASE_URI_FORMAT = '{0}{1}'

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
      sys.stderr.write(colored('Unpacked {0}'.format(zipped_file), 'green') + '\n')

def base_uri(args):
  if not args.singularity_uri_base:
    exit("Specify a base uri for Singularity (-u)")
  uri_prefix = "" if args.singularity_uri_base.startswith(("http://", "https://")) else "http://"
  uri = BASE_URI_FORMAT.format(uri_prefix, args.singularity_uri_base)
  return uri

