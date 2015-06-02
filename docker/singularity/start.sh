#!/bin/bash

PATH=/usr/local/singularity/bin:/usr/local/sbin:/usr/sbin:/sbin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/bin:/usr/bin
MESOS_HOME=/usr/local
MESOS_NATIVE_LIBRARY=/usr/local/lib/libmesos.so
PORT=7092
DEFAULT_URI_BASE="http://localhost:${SINGULARITY_PORT:=7099}/singularity"


[[ ! ${SINGULARITY_PORT:-} ]] || args+=( -Ddw.server.connector.port="$SINGULARITY_PORT" )
[[ ! ${LOAD_BALANCER_URI:-} ]] || args+=( -Ddw.loadBalancerUri="$LOAD_BALANCER_URI")

args+=( -Xmx512m )
args+=( -Djava.net.preferIPv4Stack=true )
args+=( -Ddw.mesos.master="${SINGULARITY_MASTER:=zk://localhost:2181/mesos}" )
args+=( -Ddw.zookeeper.quorum="${SINGULARITY_ZK:=localhost:2181}" )
args+=( -Ddw.ui.baseUrl="${SINGULARITY_URI_BASE:=$DEFAULT_URI_BASE}" )


exec java "${args[@]}" -jar /etc/singularity/singularity.jar server /etc/singularity/singularity.yaml