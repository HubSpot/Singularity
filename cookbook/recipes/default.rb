user node[:singularity][:user] do
  supports(manage_home: true)
  home node[:singularity][:home]
end

include_recipe 'mesos'
include_recipe 'mesos::master'
include_recipe 'mesos::slave'
include_recipe "singularity::mysql"
include_recipe "singularity::java"
include_recipe "singularity::build"
include_recipe "singularity::install"
include_recipe "singularity::configure"
include_recipe 'docker' if node[:docker][:enabled]
include_recipe 'singularity::logrotate'
