#!/bin/bash -x
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Fail fast and fail hard.
set -eo pipefail

# Get the maven version right
if [ ! -f /usr/share/apache-maven-3.3.3/bin/mvn ]; then
  wget -q http://apache.spinellicreations.com/maven/maven-3/3.3.3/binaries/apache-maven-3.3.3-bin.zip -O /tmp/apache-maven-3.3.3-bin.zip
  unzip /tmp/apache-maven-3.3.3-bin.zip -d /usr/share/
fi

if [ -f /usr/bin/mvn ]; then
  rm -rf /usr/bin/mvn
fi

ln -s /usr/share/apache-maven-3.3.3/bin/mvn /usr/bin/mvn

# Upgrade docker
version=`docker -v`
if [[ $version == *"1.6"* ]]; then
  echo "Docker up to date"
else
  rm -rf `which docker`
  wget -qO- https://get.docker.com/ | sh
fi

echo "ISOLATION=cgroups/cpu,cgroups/mem" >> /etc/default/mesos-slave