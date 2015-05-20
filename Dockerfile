FROM mesosphere/mesos:0.21.1-1.1.ubuntu1404

# Java Version
ENV JAVA_VERSION_MAJOR 8
ENV JAVA_VERSION_MINOR 45
ENV JAVA_VERSION_BUILD 14
ENV JAVA_PACKAGE       server-jre

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y curl tar && \
    curl -kLOH "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
    http://download.oracle.com/otn-pub/java/jdk/${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-b${JAVA_VERSION_BUILD}/${JAVA_PACKAGE}-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar.gz &&\
    gunzip ${JAVA_PACKAGE}-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar.gz &&\
    tar -xf ${JAVA_PACKAGE}-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar -C /opt &&\
    rm ${JAVA_PACKAGE}-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar &&\
    ln -s /opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR} /opt/jdk &&\
    ln -fs /opt/jdk/bin/java /usr/bin/java8

ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

COPY SingularityService/target/SingularityService-0.4.2-SNAPSHOT-shaded.jar /etc/singularity/singularity.jar
COPY docker/singularity/start.sh /etc/singularity/start.sh
COPY docker/singularity/singularity.yaml /etc/singularity/singularity.yaml

ENTRYPOINT ['/etc/singularity/start.sh']


