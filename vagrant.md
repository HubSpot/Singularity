# Vagrant

Singularity comes with a Vagrantfile for creating a VM for dependencies (mesos, zookeeper, mysql).

## Setup

1. Install [Vagrant <1.5](http://www.vagrantup.com/downloads-archive.html) and [Virtualbox](https://www.virtualbox.org/wiki/Downloads).
2. Ensure these Vagrant plugins are installed: (You can install vagrant plugins by running `vagrant plugin install PLUGIN_NAME`)
 - vagrant-berkshelf
 - vagrant-omnibus
 - vagrant-hosts
 - vagrant-hostsupdater
3. Start the vagrant box: `vagrant up`
4. Verify Mesos UI is accessible at [http://vagrant-singularity:5050/](http://vagrant-singularity:5050/), ZK at zk://vagrant-singularity:2181, and mysql at vagrant-singularity:3306 (see Vagrantfile for mysql password)
5. Ensure Singularity has been built: `mvn clean package`
6. Run a database migration to ensure everything is in sync: `java -jar SingularityService/target/SingularityService-VERSION.jar db migrate ./vagrant_singularity --migrations ./migrations.sql`
7. Start SingularityService using the `vagrant_singularity.yaml` config.
