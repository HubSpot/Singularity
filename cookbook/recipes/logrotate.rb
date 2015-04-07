logrotate_app 'singularity' do
  path          node[:singularity][:log_file]
  if node[:singularity][:size]
    size        node[:singularity][:size]
  else
    frequency   node[:singularity][:frequency]
  end
  rotate        node[:singularity][:logs_to_keep]
  create        '644 root root'
  template_mode '0644'
  options       %w(copytruncate
                   missingok
                   compress
                   delaycompress)
end
