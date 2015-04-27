include_recipe 'maven'

maven 'SingularityService' do
  group_id  'com.hubspot'
  classifier 'shaded'
  version   node['singularity']['version']
  dest      "#{node[:singularity][:home]}/bin/"
end
