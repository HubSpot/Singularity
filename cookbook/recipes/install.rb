[
  node[:singularity][:conf_dir],
  node[:singularity][:log_dir],
  node[:singularity][:home],
  "#{node[:singularity][:home]}/mysql",
  "#{node[:singularity][:home]}/bin"
].each { |cur_dir| directory cur_dir }

remote_file "#{node[:singularity][:home]}/bin/singularity.jar" do
  mode   0644
  source "file://#{Chef::Config[:file_cache_path]}/Singularity/" \
         'SingularityService/target/' \
         "SingularityService-#{node[:singularity][:version]}-SNAPSHOT-" \
         'shaded.jar'
end
