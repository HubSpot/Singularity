import os
import sys

CAT_COMMAND_FORMAT = 'xargs -n {0} cat < {1}'
def cat_files(args, all_logs):
    if all_logs:
        catlist_filename = '{0}/.catlist'.format(args.dest)
        create_catlist(args, all_logs, catlist_filename)
        command = CAT_COMMAND_FORMAT.format(len(all_logs), catlist_filename)
        sys.stdout.write(os.popen(command).read() + '\n')
    else:
        sys.stderr.write(colored('No log files found\n', 'magenta'))

def create_catlist(args, all_logs, catlist_filename):
  catlist_file = open(catlist_filename, 'wb')
  for log in all_logs:
    catlist_file.write('{0}\n'.format(log))
  catlist_file.close()

def remove_catlist(catlist_filename):
  if os.path.isfile(catlist_filename):
    os.remove(catlist_filename)

