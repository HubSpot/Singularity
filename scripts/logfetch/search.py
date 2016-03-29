import os
import re
import fnmatch
from logfetch_base import log, is_in_date_range
from termcolor import colored

def find_cached_logs(args):
    matching_logs = []
    log_fn_match = get_matcher(args)
    for filename in os.listdir(args.dest):
        if fnmatch.fnmatch(filename, log_fn_match) and in_date_range(args, filename):
            log(colored('Including log {0}\n'.format(filename), 'blue'), args, True)
            matching_logs.append('{0}/{1}'.format(args.dest, filename))
        else:
            log(colored('Excluding log {0}, not in date range\n'.format(filename), 'magenta'), args, True)
    return matching_logs
            

def in_date_range(args, filename):
    timestamps = re.findall(r"-\d{13}-", filename)
    if timestamps:
        return is_in_date_range(args, int(str(timestamps[-1]).replace("-", "")[0:-3]))
    else:
        return True

def get_matcher(args):
    if args.taskId:
        if 'filename' in args.file_pattern and args.logtype:
            return '{0}*{1}*'.format(args.taskId, args.logtype)
        else:
            return '{0}*'.format(args.taskId)
    elif args.deployId and args.requestId:
        if 'filename' in args.file_pattern and args.logtype:
            return '{0}-{1}*{2}*'.format(args.requestId, args.deployId, args.logtype)
        else:
            return '{0}-{1}*'.format(args.requestId, args.deployId)
    else:
        if 'filename' in args.file_pattern and args.logtype:
            return '{0}*{1}*'.format(args.requestId, args.logtype)
        else:
            return '{0}*'.format(args.requestId)