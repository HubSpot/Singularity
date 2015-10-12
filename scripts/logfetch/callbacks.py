import sys
from termcolor import colored

CALLBACK_FORMAT = '{0}/{1}'

def generate_callback(request, destination, filename, chunk_size, verbose):
  path = CALLBACK_FORMAT.format(destination, filename) if destination else filename

  def callback(response, **kwargs):
    with open(path, 'wb') as f:
      for chunk in response.iter_content(chunk_size):
        f.write(chunk)
      if verbose:
        sys.stderr.write(colored('Downloaded ', 'green') + colored(path, 'white') + '\n')
      else:
        sys.stderr.write(colored('.', 'green'))
      f.close()

  return callback

