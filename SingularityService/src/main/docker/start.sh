#!/bin/bash

PATH=/usr/local/singularity/bin:/usr/local/sbin:/usr/sbin:/sbin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/bin:/usr/bin

if [ ${DOCKER_HOST} ]; then
    HOST_AND_PORT=`echo $DOCKER_HOST | awk -F/ '{print $3}'`
    DEFAULT_HOSTNAME="${HOST_AND_PORT%:*}"
    SINGULARITY_HOSTNAME="${SINGULARITY_HOSTNAME:=$DEFAULT_HOSTNAME}"
fi

[[ ! ${SINGULARITY_PORT:-} ]] || args+=( -Ddw.server.connector.port="$SINGULARITY_PORT" )
[[ ! ${LOAD_BALANCER_URI:-} ]] || args+=( -Ddw.loadBalancerUri="$LOAD_BALANCER_URI")

args+=( -Xmx${SINGULARITY_MAX_HEAP:-512m} )
args+=( -Djava.net.preferIPv4Stack=true )
args+=( -Ddw.server.applicationContextPath="${SINGULARITY_APP_CTX:=/singularity}" )
args+=( -Ddw.mesos.master="${SINGULARITY_MESOS_MASTER:=localhost:5050}" )
args+=( -Ddw.mesos.frameworkName="${SINGULARITY_MESOS_FRAMEWORK_NAME:=Singularity}" )
args+=( -Ddw.mesos.frameworkId="${SINGULARITY_MESOS_FRAMEWORK_ID:=Singularity}" )
[[ ! ${SINGULARITY_MESOS_FRAMEWORK_ROLE:-} ]] || args+=( -Ddw.mesos.frameworkRole="${SINGULARITY_MESOS_FRAMEWORK_ROLE}" )
args+=( -Ddw.mesos.defaultCpus="${SINGULARITY_MESOS_DEFAULT_CPUS:=1}" )
args+=( -Ddw.mesos.defaultMemory="${SINGULARITY_MESOS_DEFAULT_MEMORY:=128}" )
args+=( -Ddw.mesos.defaultDisk="${SINGULARITY_MESOS_DEFAULT_DISK:=1024}" )
args+=( -Ddw.zookeeper.quorum="${SINGULARITY_ZK:=localhost:2181}" )
args+=( -Ddw.zookeeper.zkNamespace="${SINGULARITY_ZK_NAMESPACE:=singularity}" )
args+=( -Ddw.ui.baseUrl="${SINGULARITY_UI_BASE:=/singularity}" )

[[ ! ${SINGULARITY_DB_USER:-} ]] || args+=( -Ddw.database.user="${SINGULARITY_DB_USER}" )
[[ ! ${SINGULARITY_DB_PASSWORD:-} ]] || args+=( -Ddw.database.password="${SINGULARITY_DB_PASSWORD}" )
[[ ! ${SINGULARITY_DB_URL:-} ]] || args+=( -Ddw.database.url="${SINGULARITY_DB_URL}" -Ddw.database.driverClass="${SINGULARITY_DB_DRIVER_CLASS:-com.mysql.jdbc.Driver}" )
[[ ! ${SINGULARITY_HOSTNAME:-} ]] || args+=( -Ddw.hostname="${SINGULARITY_HOSTNAME}" )

# SMTP
[[ ! ${SINGULARITY_SMTP_USERNAME:-} ]] || args+=( -Ddw.smtp.username="${SINGULARITY_SMTP_USERNAME}" )
[[ ! ${SINGULARITY_SMTP_PASSWORD:-} ]] || args+=( -Ddw.smtp.password="${SINGULARITY_SMTP_PASSWORD}" )
[[ ! ${SINGULARITY_SMTP_HOST:-} ]] || args+=( -Ddw.smtp.host="${SINGULARITY_SMTP_HOST}" )
[[ ! ${SINGULARITY_SMTP_PORT:-} ]] || args+=( -Ddw.smtp.port="${SINGULARITY_SMTP_PORT}" )

[[ ! ${SINGULARITY_PERSIST_HISTORY_EVERY_SECONDS:-} ]] || args+=( -Ddw.persistHistoryEverySeconds="${SINGULARITY_PERSIST_HISTORY_EVERY_SECONDS}" )

if [[ "${SINGULARITY_DB_MIGRATE:-}" != "" ]]; then
    echo "Running: java ${args[@]} -jar /SingularityService.jar db migrate /etc/singularity/singularity.yaml --migrations /etc/singularity/migrations.sql"
    java "${args[@]}" -jar /SingularityService.jar db migrate /etc/singularity/singularity.yaml --migrations /etc/singularity/migrations.sql
fi

echo "Running: java ${args[@]} -jar /SingularityService.jar $*"
exec java "${args[@]}" -jar /SingularityService.jar $*
