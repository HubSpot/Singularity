import os
import sys
from termcolor import colored

GREP_COMMAND_FORMAT = '{0} {1}'
DEFAULT_GREP_COMMAND = 'grep --color=always \'{0}\''

def grep_files(args, all_logs):
  if not args.silent:
    sys.stderr.write('\n')
  if args.grep:
    if all_logs:
      all_logs.sort()
      for log in all_logs:
        command = grep_command(args, log)
        output = os.popen(command).read()
        if output is not None and output != '':
          if not args.silent:
            sys.stderr.write(colored(log, 'cyan') + '\n')
          sys.stdout.write(output)

      if not args.silent:
        sys.stderr.write(colored('Finished grep, exiting', 'green') + '\n')
    else:
      sys.stderr.write(colored('No logs found\n', 'magenta'))

def grep_command(args, filename):
  if 'grep' in args.grep:
    return GREP_COMMAND_FORMAT.format(args.grep, filename)
  else:
    return GREP_COMMAND_FORMAT.format(DEFAULT_GREP_COMMAND.format(args.grep), filename)
