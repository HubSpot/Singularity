docker_enabled = node[:docker][:enabled]
docker_package_version = node[:docker][:package_version]

node.set['apt']['compile_time_update'] = true
include_recipe 'apt' if node.platform_family == 'debian'

if docker_enabled
  apt_package "docker.io" do
    action :install
    version docker_package_version
  end
end
