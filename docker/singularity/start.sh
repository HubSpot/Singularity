#!/bin/bash

env PATH=/usr/local/singularity/bin:/usr/local/sbin:/usr/sbin:/sbin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/bin:/usr/bin
env MESOS_HOME=/usr/local
env MESOS_NATIVE_LIBRARY=/usr/local/lib/libmesos.so
env PORT=7092

exec java \
  -Xmx512m \
  -Djava.net.preferIPv4Stack=true \
  -Ddw.mesos.master=$SINGULARITY_MASTER \
  -Ddw.zookeeper.quorum=$SINGULARITY_ZK \
  -Ddw.ui.baseUrl="http://$SINGULARITY_HOSTNAME:7099/singularity" \
  -jar /etc/singularity/singularity.jar \
  server /etc/singularity/singularity.yaml