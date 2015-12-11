import os
import sys
from termcolor import colored

def cat_files(args, all_logs):
    if all_logs:
        all_logs.sort()
        for log in all_logs:
          if not args.silent:
            sys.stderr.write(colored(log, 'cyan') + '\n')
          command = 'cat {0}'.format(log)
          sys.stdout.write(os.popen(command).read() + '\n')
    else:
        if not args.silent:
          sys.stderr.write(colored('No log files found\n', 'magenta'))
