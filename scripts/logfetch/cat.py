import os
import sys
import subprocess
from logfetch_base import log, get_timestamp_string
from termcolor import colored

def cat_files(args, all_logs):
    log('\n', args, False)
    if all_logs:
        all_logs.sort()
        for filename in all_logs:
            log(colored(get_timestamp_string(filename) + ' => ' + filename, 'cyan') + '\n', args, not args.show_file_info)
            if filename.endswith('.gz') or filename.endswith('.bz2'):
                cat = subprocess.Popen(['cat', filename], stdout=subprocess.PIPE)
                if filename.endswith('.gz'):
                    content = subprocess.Popen(['zcat'], stdin=cat.stdout)
                else:
                    content = subprocess.Popen(['bzip2', '-dc'], stdin=cat.stdout)
                content.communicate()
            else:
                cat = subprocess.Popen(['cat', filename])
                cat.communicate()
            sys.stdout.write('\n')
    else:
            log(colored('No log files found\n', 'magenta'), args, False)
