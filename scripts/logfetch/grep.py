import os
from termcolor import colored

GREP_COMMAND_FORMAT = 'xargs -n {0} grep \'{1}\' < {2}'

def grep_files(args, all_logs):
  if args.grep:
    greplist_filename = '{0}/.greplist'.format(args.dest)
    create_greplist(args, all_logs, greplist_filename)
    grep_command = GREP_COMMAND_FORMAT.format(len(all_logs), args.grep, greplist_filename)
    print colored('Running "{0}" this might take a minute'.format(grep_command), 'blue')
    print os.popen(grep_command).read()
    remove_greplist(greplist_filename)
    print colored('Finished grep, exiting', 'green')

def create_greplist(args, all_logs, greplist_filename):
  greplist_file = open(greplist_filename, 'wb')
  for log in all_logs:
    greplist_file.write('{0}\n'.format(log))
  greplist_file.close()

def remove_greplist(greplist_filename):
  if os.path.isfile(greplist_filename):
    os.remove(greplist_filename)

