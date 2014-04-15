#!/bin/bash
#
# This script was automatically generated.
# Task ID: {{ taskId }}
#

# load system-wide profile.d
if [[ -d /etc/profile.d ]]; then
  for i in /etc/profile.d/*.sh; do
    source $i >/dev/null 2>&1
    echo "Sourced $i"
  done
else
  echo "No /etc/profile.d to source"
fi

# load env vars
if [[ -f deploy.env ]]; then
  source deploy.env
  echo "Sourced deploy-specific environment variables"
else
  echo "No deploy-specific environment variables"
fi

cd app/

# load artifact's profile.d
if [[ -d .profile.d ]]; then
    for FILE in $(ls .profile.d/*); do
        source "$FILE"
        echo "Sourced $FILE"
    done
else
  echo "No deploy-specific profile.d"
fi

if [[ "$USE_ULIMIT" == "1" ]]; then
  echo "Setting max memory via ulimit -m $(($DEPLOY_MEM * 1024))"
  ulimit -m $(($DEPLOY_MEM * 1024))
fi

# execute command
exec su - {{ user }} -c {{ cmd }} >> ../{{ logfile }} 2>&1
