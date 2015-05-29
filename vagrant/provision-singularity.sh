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

function install_singularity_config {
  mkdir -p /etc/singularity
  cat > /etc/singularity/singularity.yaml <<EOF
# singularity-related config:
server:
  type: simple
  applicationContextPath: /singularity
  connector:
    type: http
    port: 7099
  requestLog:
    appenders:
      - type: file
        currentLogFilename: ../logs/access.log
        archivedLogFilenamePattern: ../logs/access-%d.log.gz

database:
  driverClass: com.mysql.jdbc.Driver
  user: singularity
  password: 9thlevel
  url: jdbc:mysql://localhost:3306/singularity

mesos:
  master: zk://localhost:2181/mesos
  defaultCpus: 1
  defaultMemory: 128
  frameworkName: Singularity
  frameworkId: Singularity
  frameworkFailoverTimeout: 1000000

zookeeper:
  quorum: localhost:2181
  zkNamespace: singularity
  sessionTimeoutMillis: 60000
  connectTimeoutMillis: 5000
  retryBaseSleepTimeMilliseconds: 1000
  retryMaxTries: 3

logging:
  loggers:
    "com.hubspot.singularity" : DEBUG

ui:
  title: Singularity (vagrant)
  baseUrl: http://vagrant-singularity:7099/singularity

loadBalancerUri: http://vagrant-singularity:8080/baragon/v2/request

enableCorsFilter: true

EOF
}

function install_runnable_config {
  mkdir -p /usr/share/mesos/logwatcher
  mkdir -p /usr/share/mesos/s3
  mkdir -p /usr/share/mesos/artifacts
  cat > /etc/singularity.base.properties <<EOF
root.log.directory=/etc/singularity
logwatcher.metadata.directory=/usr/share/mesos/logwatcher
s3uploader.metadata.directory=/usr/share/mesos/s3
EOF
  cat > /etc/singularity.executor.properties <<EOF
executor.global.task.definition.directory=/usr/share/mesos
executor.default.user=root
executor.logrotate.to.directory=logs
executor.s3.uploader.bucket=bucket
executor.s3.uploader.pattern=%requestId/%Y/%m/%taskId_%index-%s%fileext
executor.logrotate.command=/usr/sbin/logrotate
EOF
cat > /etc/singularity.s3base.yaml <<EOF
artifactCacheDirectory: /usr/share/mesos/artifacts
EOF
  cat > /usr/local/bin/singularity-executor <<EOF
#!/bin/bash
exec java -Djava.library.path=/usr/local/lib -jar /singularity/SingularityExecutor/target/SingularityExecutor-*-shaded.jar
EOF
  chmod 755 /usr/local/bin/singularity-executor
}

function build_singularity {
  # lame hack to install a recent mvn, thanks ubuntu...
  if [ ! -f /usr/share/apache-maven-3.3.3/bin/mvn ]; then
    wget -q http://apache.spinellicreations.com/maven/maven-3/3.3.3/binaries/apache-maven-3.3.3-bin.zip -O /tmp/apache-maven-3.3.3-bin.zip

    unzip /tmp/apache-maven-3.3.3-bin.zip -d /usr/share/
  fi

  cd /singularity
  sudo -u vagrant HOME=/home/vagrant /usr/share/apache-maven-3.3.3/bin/mvn clean package -DskipTests
}

function install_singularity {
  mkdir -p /var/log/singularity
  mkdir -p /usr/local/singularity/bin
  cp /singularity/SingularityService/target/SingularityService-*-SNAPSHOT-shaded.jar /usr/local/singularity/bin/singularity.jar
  cat > /usr/local/singularity/bin/migrate_singularity_db.sh <<EOF
#!/bin/bash -x
# Uses dropwizard liquibase integration to update singularity mysql db tables
java -jar /usr/local/singularity/bin/singularity.jar db migrate /etc/singularity/singularity.yaml --migrations mysql/migrations.sql
EOF

  chmod +x /usr/local/singularity/bin/migrate_singularity_db.sh

  cat > /etc/init/singularity.conf <<EOF
#!upstart
description "Singularity Service"

env PATH=/usr/local/singularity/bin:/usr/local/sbin:/usr/sbin:/sbin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/bin:/usr/bin
env MESOS_HOME=/usr/local
env MESOS_NATIVE_LIBRARY=/usr/local/lib/libmesos.so
env PORT=7092

start on stopped rc RUNLEVEL=[2345]

respawn

exec java -Xmx512m -Djava.net.preferIPv4Stack=true -jar /usr/local/singularity/bin/singularity.jar server /etc/singularity/singularity.yaml >> /var/log/singularity/singularity.log 2>&1
EOF
}

function migrate_db {
  /usr/local/singularity/bin/migrate_singularity_db.sh
}

function stop_singularity {
  set +e  # okay if this fails (i.e. not installed)
  service singularity stop
  set -e
}

function start_singularity {
  service singularity start
}

function docker_upgrades {
  version=`docker -v`
  if [[ $version == *"1.6"* ]]
  then
    echo "Docker up to date"
  else
    wget -qO- https://get.docker.com/ | sh
  fi
  echo "ISOLATION=cgroups/cpu,cgroups/mem" >> /etc/default/mesos-slave
}

stop_singularity
docker_upgrades
install_singularity_config
build_singularity
install_singularity
migrate_db
install_runnable_config
start_singularity

echo "The Singularity Web UI is available at http://vagrant-singularity:7099/singularity/"
