directory '/etc/mesos-master' do
  owner 'root'
  group 'root'
  action :create
end

node[:mesos][:master].each do |key, value|
  file "/etc/mesos-master/#{key}" do
    owner 'root'
    group 'root'
    mode '0644'
    action :create
    content value
  end
end

service "mesos-master" do
  provider Chef::Provider::Service::Upstart
  action :start
end