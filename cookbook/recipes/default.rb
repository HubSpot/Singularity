include_recipe "singularity::hosts"
include_recipe "singularity::mesos"
include_recipe "singularity::mesos_master"
include_recipe "singularity::mesos_slave"
include_recipe "singularity::mysql"
include_recipe "singularity::java"
include_recipe 'docker' if node[:docker][:enabled]
