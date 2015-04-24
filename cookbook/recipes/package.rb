maven 'singularity' do
  group_id  'com.hubspot'
  version   node['singularity']['version']
  dest      node['singularity']['home']
end
