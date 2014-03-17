# -*- mode: ruby -*-
# vi: set ft=ruby :
# vagrant plugins required:
# vagrant-berkshelf, vagrant-omnibus, vagrant-hosts
Vagrant.configure("2") do |config|
  config.vm.box = "opscode_ubuntu-12.04_provisionerless"
  config.vm.box_url = "https://opscode-vm-bento.s3.amazonaws.com/vagrant/opscode_ubuntu-12.04_provisionerless.box"

  config.vm.hostname = 'vagrant-singularity'

  # enable plugins
  config.berkshelf.enabled = true
  config.omnibus.chef_version = :latest

  # if you want to use vagrant-cachier,
  # please activate below.
  config.cache.auto_detect = true

  mysql_password = "mesos7mysql"
  private_ip = '192.168.33.10'

  config.vm.network :private_network, ip: private_ip

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
        :version => "0.14.0"
      }
    }
  end
end
