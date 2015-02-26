::Chef::Recipe.send(:include, OpenSSLCookbook::Password)

server_root_password = (
  if File.exist?('/etc/mysql-default/root_password')
    File.read('/etc/mysql-default/root_password').chomp
  else
    secure_password
  end
)

# For some reason, due to Chef's strange and arbitrary processing order rules,
# this needs to be here even though it's in the mysql2_chef_gem LWRP.
include_recipe 'build-essential::default'

mysql2_chef_gem 'default' do
  action :install
end

mysql_service 'default' do
  port node[:mysql][:port]
  bind_address node[:mysql][:bind_address]
  version node[:mysql][:version]
  initial_root_password server_root_password
  action [:create, :start]
end

mysql_client 'default' do
  action :create
end

file '/etc/mysql-default/root_password' do
  content "#{server_root_password}\n"
  user 'root'
  group 'root'
  mode 0600
  action :create_if_missing
end

mysql_connection_info = {
  :host     => '127.0.0.1',
  :username => 'root',
  :password => server_root_password
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
