docker_enabled = node[:docker][:enabled]
docker_package_version = node[:docker][:package_version]

if docker_enabled
  apt_package "docker.io" do
    action :install
    version docker_package_version
  end
end