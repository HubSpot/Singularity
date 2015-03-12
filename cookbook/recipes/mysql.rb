mysql2_chef_gem 'default' do
  action :install
end

mysql_service 'default' do
  port node[:mysql][:port]
  bind_address node[:mysql][:bind_address]
  version node[:mysql][:version]
  initial_root_password node[:mysql][:server_root_password]
  action [:create, :start]
end

mysql_client 'default' do
  action :create
end

mysql_connection_info = {
  :host     => '127.0.0.1',
  :username => 'root',
  :password => node[:mysql][:server_root_password]
}

mysql_database node[:singularity][:database][:db_name] do
  connection mysql_connection_info
  action :create
end

mysql_database_user node[:singularity][:database][:username] do
  connection mysql_connection_info
  password node[:singularity][:database][:password]
  database_name node[:singularity][:database][:db_name]
  host '%'
  privileges [:all]
  action [:grant]
end
