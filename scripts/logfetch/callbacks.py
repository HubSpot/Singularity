import sys
from termcolor import colored

progress = 0
goal = 0

CALLBACK_FORMAT = '{0}/{1}'

def update_progress_bar(progress):
  bar_length = 30
  global goal
  percent = float(progress) / goal
  hashes = '#' * int(round(percent * bar_length))
  spaces = ' ' * (bar_length - len(hashes))
  if percent > .9:
    color = 'green'
  elif percent > .5:
    color = 'cyan'
  elif percent > .25:
    color = 'yellow'
  else:
    color = 'white'
  sys.stderr.write("\rDownload Progress: [" + colored("{0}".format(hashes + spaces), color) + "] {0}%".format(int(round(percent * 100))))
  sys.stderr.flush()


def generate_callback(request, destination, filename, chunk_size, verbose):
  path = CALLBACK_FORMAT.format(destination, filename) if destination else filename

  def callback(response, **kwargs):
    global progress
    global goal
    with open(path, 'wb') as f:
      for chunk in response.iter_content(chunk_size):
        f.write(chunk)
      progress += 1
      if verbose:
        sys.stderr.write(colored('Downloaded log {0}/{1} '.format(progress, goal), 'green') + colored(path, 'white') + '\n')
      else:
        update_progress_bar(progress)

  return callback