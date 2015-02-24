include_recipe 'singularity::build'

["#{node[:singularity][:home]}/bin",
 "#{node[:singularity][:data_dir]}/executor-tasks"].each do |dir|
  directory dir do
    owner     node[:singularity][:user]
    group     node[:singularity][:user]
    mode      0755
    recursive true
    action    :create
  end
end

%w(executor
   base
   s3base).each do |prop_file|
  template "/etc/singularity.#{prop_file}.properties" do
    source "singularity.#{prop_file}.properties.erb"
    owner  'root'
    group  'root'
    mode   0644
    variables(creds: data_bag_item('secrets', 'aws_credentials')['Singularity'])
  end
end

remote_file "#{node[:singularity][:home]}/bin/SingularityExecutor" do
  mode     0755
  source   "file://#{Chef::Config[:file_cache_path]}/Singularity/" \
           'SingularityExecutor/target/SingularityExecutor'
end
