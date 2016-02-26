import os
import sys
import subprocess
from logfetch_base import log
from termcolor import colored

def cat_files(args, all_logs):
    log('\n', args, False)
    if all_logs:
        all_logs.sort()
        for filename in all_logs:
            log('=> ' + colored(filename, 'cyan') + '\n', args, False)
            if filename.endswith('.gz'):
                cat = subprocess.Popen(['cat', filename], stdout=subprocess.PIPE)
                content = subprocess.Popen(['zcat'], stdin=cat.stdout)
                content.communicate()
            else:
                cat = subprocess.Popen(['cat', filename])
                cat.communicate()
            sys.stdout.write('\n')
    else:
            log(colored('No log files found\n', 'magenta'), args, False)
