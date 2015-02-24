user node[:singularity][:user] do
  supports(manage_home: true)
  home "/home/#{node[:singularity][:user]}"
end

include_recipe 'mesos'
include_recipe 'mesos::master'
include_recipe 'mesos::slave'
include_recipe "singularity::mysql"
include_recipe "singularity::java"
