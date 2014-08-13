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

# Ensure java 7 is the default java.
update-alternatives --set java /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java

function install_singularity_config {
  mkdir -p /etc/singularity
  cat > /etc/singularity/singularity.yaml <<EOF
# singularity-related config:
server:
  type: simple
  applicationContextPath: /singularity/v2
  connector:
    type: http
    port: 7092
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
  maxNumInstancesPerRequest: 10
  maxNumCpusPerInstance: 3
  maxNumCpusPerRequest: 10
  maxMemoryMbPerInstance: 24000
  maxMemoryMbPerRequest: 160000

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

smtp:
  username: "mailuser"
  password: "mailpassword"
  host: "mail.server.com"
  port: 25
  from: "singularity@mycompany.com"
  admins:
    - singularity-admin@company.com
  includeAdminsOnAllMails: true
    
s3:
  s3SecretKey: "my s3 secret key"
  s3AccessKey: "my s3 access key"
  s3Bucket: "singularity-deployable-item-taks-logs"
  s3KeyFormat: "%requestId/%Y/%m/%taskId_%index-%s%fileext"
EOF
}

function build_singularity {
  cd /singularity
  mv clean package
}

function run_singularity {
  cd /singularity
  MESOS_HOME=/usr/local MESOS_NATIVE_LIBRARY=/usr/local/lib/libmesos.so PORT=7092 java -Xmx512m -Djava.net.preferIPv4Stack=true -jar ./SingularityService/target/SingularityService-0.3.8-SNAPSHOT.jar server /etc/singularity/singularity.yaml &
}

install_singularity_config
build_singularity
run_singularity

