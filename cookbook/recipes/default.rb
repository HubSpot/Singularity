user node[:singularity][:user] do
  supports(manage_home: true)
  home node[:singularity][:home]
end

include_recipe 'mesos'
include_recipe 'mesos::master'
include_recipe "singularity::mysql"
include_recipe "singularity::java"

[node[:singularity][:conf_dir],
 node[:singularity][:log_dir],
 node[:singularity][:home],
 "#{node[:singularity][:home]}/mysql",
 "#{node[:singularity][:home]}/bin"].each { |cur_dir| directory cur_dir }

case node['singularity']['install_type']
when 'package'
  include_recipe 'singularity::package'
when 'source'
  include_recipe 'singularity::source'
else
  fail "Invalid install type: #{node['singularity']['install_type']}"
end

include_recipe "singularity::build"
include_recipe "singularity::install"
include_recipe "singularity::configure"
include_recipe 'docker' if node[:docker][:enabled]
include_recipe 'singularity::logrotate'
