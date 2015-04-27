node.default[:singularity][:baragon_url] =
  'http://localhost:' \
  "#{node['baragon']['service_yaml']['server']['connector']['port']}" \
  '/baragon/v2'

template "#{node[:singularity][:conf_dir]}/singularity.yaml" do
  source 'singularity.yaml.erb'
  owner  'root'
  group  'root'
  mode   0644
end

execute 'migrate_singularity_db' do
  command "#{node[:java][:java_home]}/bin/java " \
          "-jar #{node[:singularity][:home]}/bin/singularity.jar " \
          "db migrate #{node[:singularity][:conf_dir]}/singularity.yaml " \
          "--migrations #{node[:singularity][:home]}/mysql/migrations.sql " \
          "&& touch #{node[:singularity][:conf_dir]}/migration_ran"
  action  :nothing
end

cookbook_file "#{node[:singularity][:home]}/mysql/migrations.sql" do
  source   "migrations.sql"
  owner    'root'
  group    'root'
  mode     0644
  notifies :restart, 'mysql_service[default]', :immediately
  notifies :run, 'execute[migrate_singularity_db]'
end

template '/etc/init/singularity.conf' do
  source 'singularity-init.erb'
  owner  'root'
  group  'root'
  mode   0644
end

service 'singularity' do
  provider Chef::Provider::Service::Upstart
  supports status: true, restart: true
  action   [:enable, :start]
  subscribes :restart,
             "remote_file[#{node[:singularity][:home]}/bin/singularity.jar]"
end
