logrotate_app 'singularity' do
  path          node[:singularity][:log_file]
  frequency     'daily'
  rotate        node[:singularity][:logs_to_keep]
  create        '644 root root'
  template_mode '0644'
  options       %w(copytruncate
                   missingok
                   compress
                   delaycompress)
end
