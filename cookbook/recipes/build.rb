directory "#{Chef::Config[:file_cache_path]}/Singularity" do
  owner node[:singularity][:user]
end

git "#{Chef::Config[:file_cache_path]}/Singularity" do
  repository 'https://github.com/HubSpot/Singularity.git'
  reference  node[:singularity][:git_ref]
  user       node[:singularity][:user]
  action     :export
end

execute 'build_singularity' do
  action  :run
  # Maven (or rather npm) has issues with
  # being run as root.
  user    node[:singularity][:user]
  environment('HOME' => '/home/singularity')
  command '/usr/bin/mvn clean package -DskipTests'
  creates "#{Chef::Config[:file_cache_path]}/Singularity/" \
          'SingularityService/target/' \
          "SingularityService-#{node[:singularity][:version]}-" \
          'SNAPSHOT-shaded.jar'
  cwd     "#{Chef::Config[:file_cache_path]}/Singularity"
end
