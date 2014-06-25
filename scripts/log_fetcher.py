import argparse
import ConfigParser
import sys
import os
import grequests
import requests

class FakeSectionHead(object):
  def __init__(self, fp, section_name):
    self.fp = fp
    self.sechead = '[%s]\n' % section_name
  
  def readline(self):
    if self.sechead:
      try: return self.sechead
      finally: self.sechead = None
    else: return self.fp.readline()
  
def exit(parser, reason):
  if parser:
    print parser.print_help()
  print reason
  sys.exit(1)
    
def main(parser, args):
  if args.taskId:
    singularity_path = "task/%s" % args.taskId
  elif args.deployId:
    if not args.requestId:
      exit(parser, "RequestId requires DeployId")
    singularity_path = "request/%s/deploy/%s" % (args.requestId, args.deployId)
  elif args.requestId:
    singularity_path = "request/%s" % args.requestId 
  else:
    exit(parser, "Specify one of taskId, requestId and deployId, or requestId")
    
  if not args.singularity_uri_base:
    exit(parser, "Specify a base uri for Singularity")

  uri_prefix = ""
  
  if not args.singularity_uri_base.startswith(("http://", "https://")):
    uri_prefix = "http://"
    
  singularity_uri = "%s%s/logs/%s" % (uri_prefix, args.singularity_uri_base, singularity_path)
  
  print "fetching log metadata from %s" % singularity_uri
  
  singularity_response = requests.get(singularity_uri)
  
  if singularity_response.status_code < 199 or singularity_response.status_code > 299:
    exit(None, "Singularity responded with an invalid status code (%s)" % singularity_response.status_code)
  
  s_json = singularity_response.json()
  
  print "found %s log files" % len(s_json)
  
  async_requests = [grequests.AsyncRequest('GET', log_file['getUrl'], callback=generate_callback(log_file['getUrl'], args.dest, log_file['key'][log_file['key'].rfind('/') + 1:], args.chunk_size)) for log_file in s_json]
  
  grequests.map(async_requests, stream=True, size=args.num_parallel_fetches)
  
def generate_callback(request, destination, filename, chunk_size):
  if destination:
    path = "%s/%s" % (destination, filename)
  else:
    path = filename
    
  def callback(response, **kwargs):
    print "got response %s for %s (%s)" % (response.status_code, request, kwargs)
    with open(path, 'wb') as f:
      for chunk in response.iter_content(chunk_size):
        f.write(chunk)
    print "finished writing %s" % path
  
  return callback

if __name__ == "__main__":
  conf_parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter, add_help=False)
  conf_parser.add_argument("-c", "--conf_file", help="Specify config file", metavar="FILE")
  args, remaining_argv = conf_parser.parse_known_args()

  conf_file = os.path.expanduser("~/.log_fetcherrc")
  
  if args.conf_file:
    conf_file = args.conf_file
    
  config = ConfigParser.SafeConfigParser()
  
  defaults = { "num_parallel_fetches" : 5, "chunk_size" : 8192, "dest" : "" }
  
  try:
    config.readfp(FakeSectionHead(open(conf_file), 'Defaults'))
    defaults.update(dict(config.items("Defaults")))
  except Exception, err:
    print "Couldn't load config from %s due to %s" % (conf_file, err)
  
  parser = argparse.ArgumentParser(parents=[conf_parser], description="Fetch log files from Singularity. One can specify either a TaskId, RequestId and DeployId, or RequestId",
          prog="log_fetcher")
          
  parser.set_defaults(**defaults)
  
  parser.add_argument("-t", "--taskId", help="TaskId of task to fetch logs for", metavar="taskId")
  parser.add_argument("-r", "--requestId", help="RequestId of request to fetch logs for", metavar="requestId")
  parser.add_argument("-d", "--deployId", help="DeployId of task to fetch logs for", metavar="deployId")
  parser.add_argument("--dest", help="Destination directory", metavar="DIR")
  parser.add_argument("-n", "--num-parallel-fetches", help="Number of fetches to make at once", type=int, metavar="INT")
  parser.add_argument("-cs", "--chunk-size", help="Chunk size for writing from response to filesystem", type=int, metavar="INT")
  parser.add_argument("-s", "--singularity-uri-base", help="The base for singularity (eg. http://localhost:8080/singularity/v1)", metavar="URI")
  
  args = parser.parse_args(remaining_argv)
  
  main(parser, args)
  