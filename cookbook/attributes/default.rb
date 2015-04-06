if node[:network][:interfaces][:eth1]
  private_ip = node[:network][:interfaces][:eth1][:addresses].detect{|k,v| v[:family] == "inet" }.first
else
  private_ip = node[:network][:interfaces][:eth0][:addresses].find do |_k, v|
    v[:family] == 'inet'
  end.first
end

default[:singularity] = {
  :data_dir                 => '/var/lib/singularity',
  :log_dir                  => '/var/log/singularity',
  :conf_dir                 => '/etc/singularity',
  :base_url                 => "http://#{node[:fqdn]}:7099/singularity",
  :app_mysql_defaults       => { 'adapter' => 'mysql2',
                                 'pool' => 20,
                                 'timeout' => 5000 },
  :database                 => { :db_name => "singularity",
                                 :username => "singularity",
                                 :password => "9thlevel" }
}

default[:singularity][:home] = '/usr/local/singularity'
default[:singularity][:log_level] = 'INFO'

default[:mesos] = {
  :package_version => "0.21.0-1.0.ubuntu1404",
  :common => {
    :ip => private_ip,
  },
  :master => {
    :cluster => 'vagrant-singularity',
  },
  :slave => {
    :containerizers => 'docker,mesos',
    :switch_user => 'false'
  },
  :slave_resources => {},
  :slave_attributes => {}
}

default[:docker] = {
  :enabled => true,
  :package_version => "1.0.1~dfsg1-0ubuntu1~ubuntu0.14.04.1",
}

default[:mysql] = {
  :port => '3306',
  :bind_address => '0.0.0.0',
  :version => '5.5',
}

default['baragon']['service_yaml']['server']['connector']['port'] = 8088

override['java']['install_flavor'] = "oracle"
override['java']['jdk_version'] = "7"
override['java']['oracle']['accept_oracle_download_terms'] = true
override['java']['set_default'] = true
set['java']['ark_timeout'] = 10
set['java']['ark_retries'] = 3
