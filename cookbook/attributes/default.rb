if node[:network][:interfaces][:eth1]
  private_ip = node[:network][:interfaces][:eth1][:addresses].detect{|k,v| v[:family] == "inet" }.first
else
  private_ip = node[:network][:interfaces][:eth0][:addresses].find do |_k, v|
    v[:family] == 'inet'
  end.first
end

default[:singularity] = {
  :database => {
    :db_name => "singularity",
    :username => "singularity",
    :password => "9thlevel"
  },
}

set[:mesos][:type] = 'mesosphere'
default[:mesos][:master][:zk] = 'http://localhost:2181/singularity'

default[:docker] = {
  :enabled => true,
  :package_version => "1.0.1~dfsg1-0ubuntu1~ubuntu0.14.04.1",
}

default[:mysql] = {
  :port => '3306',
  :bind_address => '0.0.0.0',
  :version => '5.5',
}

override['java']['install_flavor'] = "oracle"
override['java']['jdk_version'] = "7"
override['java']['oracle']['accept_oracle_download_terms'] = true
override['java']['set_default'] = true
set['java']['ark_timeout'] = 10
set['java']['ark_retries'] = 3
