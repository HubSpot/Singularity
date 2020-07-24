FROM ubuntu:xenial

MAINTAINER platform-infrastructure-groups@hubspot.com

RUN apt-get update && \
    echo "deb http://repos.mesosphere.com/ubuntu xenial main"  > /etc/apt/sources.list.d/mesosphere.list && \
    apt-get -y update && \
    apt-get -y --allow-unauthenticated install mesos="1.9.0-2.0.1.ubuntu1604" && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# to build - docker build -t hubspot/mesos:1.9.0 .