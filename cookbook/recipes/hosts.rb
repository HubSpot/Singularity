hostsfile_entry '127.0.1.1' do
  action :remove
end

hostsfile_entry node[:mesos][:common][:ip] do
  hostname node['hostname']
  action :create
end