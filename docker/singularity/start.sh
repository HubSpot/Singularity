#!/bin/bash

PATH=/usr/local/singularity/bin:/usr/local/sbin:/usr/sbin:/sbin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/bin:/usr/bin
MESOS_HOME=/usr/local
MESOS_NATIVE_LIBRARY=/usr/local/lib/libmesos.so

if [ ${DOCKER_HOST} ]; then
	HOST_AND_PORT=`echo $DOCKER_HOST | awk -F/ '{print $3}'`
	HOST_IP="${HOST_AND_PORT%:*}"
fi

DEFAULT_URI_BASE="http://${HOST_IP:=localhost}:${SINGULARITY_PORT:=7099}${SINGULARITY_UI_BASE:=/singularity}"


[[ ! ${SINGULARITY_PORT:-} ]] || args+=( -Ddw.server.connector.port="$SINGULARITY_PORT" )
[[ ! ${LOAD_BALANCER_URI:-} ]] || args+=( -Ddw.loadBalancerUri="$LOAD_BALANCER_URI")

args+=( -Xmx512m )
args+=( -Djava.net.preferIPv4Stack=true )
args+=( -Ddw.mesos.master="${SINGULARITY_MASTER:=zk://localhost:2181/mesos}" )
args+=( -Ddw.zookeeper.quorum="${SINGULARITY_ZK:=localhost:2181}" )
args+=( -Ddw.ui.baseUrl="${SINGULARITY_URI_BASE:=$DEFAULT_URI_BASE}" )


exec java "${args[@]}" -jar /etc/singularity/singularity.jar server /etc/singularity/singularity.yaml