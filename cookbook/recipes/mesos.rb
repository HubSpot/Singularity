apt_repository "mesosphere" do
  uri "http://repos.mesosphere.io/ubuntu"
  distribution node['lsb']['codename']
  components ["main"]
end

apt_package "mesos" do
  action :install
  version node[:mesos][:package_version]
  options "-y --force-yes"  # ghetto
end

directory '/etc/mesos' do
  owner 'root'
  group 'root'
  action :create
end

node[:mesos][:common].each do |key, value|
  file "/etc/mesos/#{key}" do
    owner 'root'
    group 'root'
    mode '0644'
    action :create
    content value
  end
end