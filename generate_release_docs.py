import argparse
import requests



def base_text(release):
    return """## Changes in `{0}`

Check out the [{0} milestone](https://github.com/HubSpot/Singularity/issues?q=milestone%3A%{0}+is%3Aclosed) to see new features / bugfixes in detail.

### New Features

### Improvements

### Bug Fixes

### Documentation


""".format(release)

def main(args):
    pulls = [p for p in requests.get('https://api.github.com/repos/HubSpot/Singularity/pulls?state=closed&per_page=200&sort=updated&direction=desc').json() if 'milestone' in p and p['milestone'] and args.release.encode('utf-8') == p['milestone']['title']]
    print 'Found {0} pull requests'.format(len(pulls))
    message = base_text(args.release)
    for p in pulls:
        message = message + '- [{0}]({1}) - {2}'.format(p['number'], p['html_url'], p['title']) + '\n'
    outfile = open('Docs/releases/{0}.md'.format(args.release), 'w')
    outfile.write(message)
    outfile.close()

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generate release docs from milestone')
    parser.add_argument('-r', '--release', dest='release', help='release version')
    args = parser.parse_args()

    main(args)