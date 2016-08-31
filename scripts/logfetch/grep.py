import sys
import subprocess
from logfetch_base import log, get_timestamp_string
from termcolor import colored

DEFAULT_GREP_COMMAND = 'grep --color=always \'{0}\''

def grep_files(args, all_logs):
    log('\n', args, False)
    if args.grep:
        if all_logs:
            all_logs.sort()
            grep_cmd = grep_command(args)
            log(colored('Running grep command ({0})\n'.format(grep_cmd), 'cyan'), args, False)
            for filename in all_logs:
                log(colored(get_timestamp_string(filename) + ' => ' + filename, 'cyan') + '\n', args, not args.show_file_info)
                content = subprocess.Popen(['cat', filename], stdout=subprocess.PIPE)
                if filename.endswith('.gz'):
                    zcat = subprocess.Popen('zcat', stdin=content.stdout, stdout=subprocess.PIPE)
                    grep = subprocess.Popen(grep_cmd, stdin=zcat.stdout, shell=True)
                elif filename.endswith('.bz2'):
                    bz = subprocess.Popen(['bzip2', '-dc'], stdin=content.stdout, stdout=subprocess.PIPE)
                    grep = subprocess.Popen(grep_cmd, stdin=bz.stdout, shell=True)
                else:
                    grep = subprocess.Popen(grep_cmd, stdin=content.stdout, shell=True)
                grep.communicate()
            log(colored('Finished grep, exiting', 'green') + '\n', args, False)
        else:
            sys.stderr.write(colored('No logs found\n', 'magenta'))

def grep_command(args):
    if 'grep' in args.grep:
        return args.grep
    else:
        return DEFAULT_GREP_COMMAND.format(args.grep)
