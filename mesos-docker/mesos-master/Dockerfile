FROM hubspot/mesos:1.0

MAINTAINER platform-infrastructure-groups@hubspot.com

CMD ["--registry=in_memory"]
ENTRYPOINT ["mesos-master"]

# to build - docker build -t hubspot/mesos-master:1.0 .