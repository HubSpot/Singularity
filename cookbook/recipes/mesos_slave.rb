include_recipe 'singularity::docker'

directory '/etc/mesos-slave' do
  owner 'root'
  group 'root'
  action :create
end

node[:mesos][:slave].each do |key, value|
  file "/etc/mesos-slave/#{key}" do
    owner 'root'
    group 'root'
    mode '0644'
    action :create
    content value
  end
end

if node[:mesos][:slave_resources]
  directory '/etc/mesos-slave/resources' do
    owner 'root'
    group 'root'
    action :create
  end

  node[:mesos][:slave_resources].each do |key, value|
    file "/etc/mesos-slave/resources/#{key}" do
      owner 'root'
      group 'root'
      mode '0644'
      action :create
      content value
    end
  end
end

if node[:mesos][:slave_attributes]
  directory '/etc/mesos-slave/attributes' do
    owner 'root'
    group 'root'
    action :create
  end

  node[:mesos][:slave_attributes].each do |key, value|
    file "/etc/mesos-slave/attributes/#{key}" do
      owner 'root'
      group 'root'
      mode '0644'
      action :create
      content value
    end
  end
end

service "mesos-slave" do
  provider Chef::Provider::Service::Upstart
  action :start
end