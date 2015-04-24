include_recipe "singularity::hosts"
include_recipe "singularity::mesos"
include_recipe "singularity::mesos_master"
include_recipe "singularity::mesos_slave"

case node['singularity']['install_type']
when 'package'
  include_recipe 'singularity::package'
when 'source'
  include_recipe 'singularity::source'
else
  fail "Invalid install type: #{node['singularity']['install_type']}"
end

include_recipe "singularity::mysql"
include_recipe "singularity::java"
include_recipe 'singularity::logrotate'
