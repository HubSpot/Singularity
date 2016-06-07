import sys
from termcolor import colored

progress = 0
goal = 0

CALLBACK_FORMAT = '{0}/{1}'

def update_progress_bar(silent, progress):
    bar_length = 30
    global goal
    percent = float(progress) / goal
    hashes = '#' * int(round(percent * bar_length))
    spaces = ' ' * (bar_length - len(hashes))
    if percent > .8:
        color = 'green'
    elif percent > .5:
        color = 'cyan'
    elif percent > .25:
        color = 'yellow'
    else:
        color = 'blue'
    if not silent: 
        sys.stderr.write("\rDownload Progress: [" + colored("{0}".format(hashes + spaces), color) + "] {0}%".format(int(round(percent * 100))))
        sys.stderr.flush()


def generate_callback(request, destination, filename, chunk_size, verbose, silent):
    def callback(response, **kwargs):
        global progress
        global goal
        path = CALLBACK_FORMAT.format(destination, filename) if destination else filename
        if 'content-encoding' in response.headers and response.headers['content-encoding'] == 'gzip' and path.endswith('.gz'):
            path = path[:-3]
        with open(path, 'wb') as f:
            for chunk in response.iter_content(chunk_size):
                f.write(chunk)
            progress += 1
            if verbose and not silent:
                sys.stderr.write(colored('Downloaded log {0}/{1} '.format(progress, goal), 'green') + colored(path, 'white') + '\n')
            else:
                update_progress_bar(silent, progress)

    return callback