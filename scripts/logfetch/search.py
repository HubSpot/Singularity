import os
import re
import sys
import fnmatch
import logfetch_base
from termcolor import colored

def find_cached_logs(args):
    matching_logs = []
    log_fn_match = get_matcher(args)
    for filename in os.listdir(args.dest):
        if fnmatch.fnmatch(filename, log_fn_match) and in_date_range(args, filename):
            if args.verbose and not args.silent:
                sys.stderr.write(colored('Including log {0}\n'.format(filename), 'blue'))
            matching_logs.append('{0}/{1}'.format(args.dest, filename))
        else:
            if args.verbose and not args.silent:
                sys.stderr.write(colored('Excluding log {0}, not in date range\n'.format(filename), 'magenta'))
    return matching_logs
            

def in_date_range(args, filename):
    timestamps = re.findall(r"-\d{13}-", filename)
    if timestamps:
        return logfetch_base.is_in_date_range(args, int(str(timestamps[-1]).replace("-", "")[0:-3]))
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