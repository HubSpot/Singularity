#!/bin/bash

PATH=/usr/local/singularity/bin:/usr/local/sbin:/usr/sbin:/sbin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/bin:/usr/bin

if [ ${DOCKER_HOST} ]; then
	HOST_AND_PORT=`echo $DOCKER_HOST | awk -F/ '{print $3}'`
	HOST_IP="${HOST_AND_PORT%:*}"
fi

DEFAULT_URI_BASE="http://${HOST_IP:=localhost}:${SINGULARITY_PORT:=7099}${SINGULARITY_UI_BASE:=/singularity}"


[[ ! ${SINGULARITY_PORT:-} ]] || args+=( -Ddw.server.connector.port="$SINGULARITY_PORT" )
[[ ! ${LOAD_BALANCER_URI:-} ]] || args+=( -Ddw.loadBalancerUri="$LOAD_BALANCER_URI")

args+=( -Xmx${SINGULARITY_MAX_HEAP:-512m} )
args+=( -Djava.net.preferIPv4Stack=true )
args+=( -Ddw.mesos.master="${SINGULARITY_MESOS_MASTER:=zk://localhost:2181/mesos}" )
args+=( -Ddw.zookeeper.quorum="${SINGULARITY_ZK:=localhost:2181}" )
args+=( -Ddw.zookeeper.zkNamespace="${SINGULARITY_ZK_NAMESPACE:=singularity}" )
args+=( -Ddw.ui.baseUrl="${SINGULARITY_URI_BASE:=$DEFAULT_URI_BASE}" )

[[ ! ${SINGULARITY_DB_USER:-} ]] || args+=( -Ddw.database.user="${SINGULARITY_DB_USER}" )
[[ ! ${SINGULARITY_DB_PASSWORD:-} ]] || args+=( -Ddw.database.password="${SINGULARITY_DB_PASSWORD}" )
[[ ! ${SINGULARITY_DB_URL:-} ]] || args+=( -Ddw.database.url="${SINGULARITY_DB_URL}" -Ddw.database.driverClass="${SINGULARITY_DB_DRIVER_CLASS:-com.mysql.jdbc.Driver}" )

[[ ! ${SINGULARITY_PERSIST_HISTORY_EVERY_SECONDS:-} ]] || args+=( -Ddw.persistHistoryEverySeconds="${SINGULARITY_PERSIST_HISTORY_EVERY_SECONDS}" )

if [[ "${SINGULARITY_DB_MIGRATE:-}" != "" ]]; then
	echo "Running: java ${args[@]} -jar /SingularityService.jar db migrate /etc/singularity/singularity.yaml --migrations /etc/singularity/migrations.sql"
	java "${args[@]}" -jar /SingularityService.jar db migrate /etc/singularity/singularity.yaml --migrations /etc/singularity/migrations.sql
fi

echo "Running: java ${args[@]} -jar /SingularityService.jar $*"
exec java "${args[@]}" -jar /SingularityService.jar $*