import os
import sys
from termcolor import colored

GREP_COMMAND_FORMAT = 'xargs -n {0} {1} < {2}'
DEFAULT_GREP_COMMAND = 'grep --color=always \'{0}\''

def grep_files(args, all_logs):
  if args.grep:
    greplist_filename = '{0}/.greplist'.format(args.dest)
    create_greplist(args, all_logs, greplist_filename)
    command = grep_command(args, all_logs, greplist_filename)
    sys.stderr.write(colored('Running "{0}" this might take a minute'.format(command), 'blue') + '\n')
    sys.stdout.write(os.popen(command).read() + '\n')
    remove_greplist(greplist_filename)
    sys.stderr.write(colored('Finished grep, exiting', 'green') + '\n')

def create_greplist(args, all_logs, greplist_filename):
  greplist_file = open(greplist_filename, 'wb')
  for log in all_logs:
    greplist_file.write('{0}\n'.format(log))
  greplist_file.close()

def remove_greplist(greplist_filename):
  if os.path.isfile(greplist_filename):
    os.remove(greplist_filename)

def grep_command(args, all_logs, greplist_filename):
  if 'grep' in args.grep:
    return GREP_COMMAND_FORMAT.format(len(all_logs), args.grep, greplist_filename)
  else:
    return GREP_COMMAND_FORMAT.format(len(all_logs), DEFAULT_GREP_COMMAND.format(args.grep), greplist_filename)
