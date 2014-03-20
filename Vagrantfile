# -*- mode: ruby -*-
# vi: set ft=ruby :
# vagrant plugins required:
# vagrant-berkshelf, vagrant-omnibus, vagrant-hosts, vagrant-hostsupdater
Vagrant.require_plugin "vagrant-berkshelf"
Vagrant.require_plugin "vagrant-omnibus"
Vagrant.require_plugin "vagrant-hosts"
Vagrant.require_plugin "vagrant-hostsupdater"

Vagrant.configure("2") do |config|
  config.vm.box = "opscode_ubuntu-12.04_provisionerless"
  config.vm.box_url = "https://opscode-vm-bento.s3.amazonaws.com/vagrant/opscode_ubuntu-12.04_provisionerless.box"

  config.vm.hostname = 'vagrant-singularity'
  private_ip = '192.168.33.11'
  mysql_password = "mesos7mysql"

  # enable plugins
  config.berkshelf.enabled = true
  config.omnibus.chef_version = :latest

  if Vagrant.has_plugin?("vagrant-cachier")
    config.cache.auto_detect = true
  end
  
  config.vm.network :private_network, ip: private_ip
  config.vm.provision :hosts do |provisioner|
    provisioner.add_host private_ip, [config.vm.hostname]
  end

  config.vm.provision :chef_solo do |chef|
    chef.log_level = :debug
    chef.add_recipe "singularity"

    # You may also specify custom JSON attributes:
    chef.json = {
      :mysql => {
        :server_root_password => "#{mysql_password}",
        :server_repl_password => "#{mysql_password}",
        :server_debian_password => "#{mysql_password}",
        :bind_address => "0.0.0.0",
        :allow_remote_root => true
      },
      :mesos => {
        :version => "0.17.0"
      }
    }
  end
end
