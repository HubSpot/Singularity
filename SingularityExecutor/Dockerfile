FROM mesosphere/mesos:1.1.1
## mesos + java used to build hubspot/singularitybase

MAINTAINER platform-infrastructure-groups@hubspot.com

# Java Version
ENV JAVA_VERSION_MAJOR 8
ENV JAVA_VERSION_MINOR 131
ENV JAVA_VERSION_BUILD 11
ENV JAVA_PACKAGE       server-jre
ENV JAVA_SHA           d54c1d3a095b4ff2b6607d096fa80163

RUN apt-get update && \
    apt-get install -y curl tar logrotate ca-certificates apt-transport-https lxc iptables && \
    apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D && \
    echo "deb https://apt.dockerproject.org/repo ubuntu-trusty main"  > /etc/apt/sources.list.d/docker.list && \
    apt-get update && \
    apt-get -y install docker-engine && \
    curl -skLOH "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
    http://download.oracle.com/otn-pub/java/jdk/${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-b${JAVA_VERSION_BUILD}/${JAVA_SHA}/${JAVA_PACKAGE}-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar.gz && \
    gunzip ${JAVA_PACKAGE}-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar.gz && \
    tar -xf ${JAVA_PACKAGE}-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar -C /opt && \
    rm ${JAVA_PACKAGE}-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar && \
    ln -s /opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR} /opt/jdk && \
    apt-get clean autoclean && \
    apt-get autoremove -y && \
    rm -rf /var/lib/{apt,dpkg,cache,log}/

RUN mkdir -p /usr/share/mesos/logwatcher && \
    mkdir -p /usr/share/mesos/s3 && \
    mkdir -p /usr/share/mesos/artifacts

ENV JAVA_HOME /opt/jdk1.8.0_131

RUN update-alternatives --install /usr/bin/java java ${JAVA_HOME}/bin/java 20000 && \
    update-alternatives --install /usr/bin/javac javac ${JAVA_HOME}/bin/javac 20000 && \
    update-alternatives --install /usr/bin/jcmd jcmd ${JAVA_HOME}/bin/jcmd 20000 && \
    update-alternatives --install /usr/bin/jar jar ${JAVA_HOME}/bin/jar 20000
