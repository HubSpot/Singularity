# Vagrant

Singularity comes with a Vagrantfile for creating a VM for dependencies (mesos, zookeeper, mysql).

## Setup

1. Install [Vagrant](http://www.vagrantup.com/downloads.html) and [Virtualbox](https://www.virtualbox.org/wiki/Downloads).
2. Ensure the following Vagrant plugins are installed. You can install vagrant plugins by running `vagrant plugin install PLUGIN_NAME`. If you use vagrant version 1.5 or later make sure to install vagrant-omnibus before vagrant-berkshelf. It seems that omnibus causes a downgrade of berkshelf version back to 1.3.7 which in turn will cause 'vagrant up' to fail.
 - vagrant-omnibus
 - vagrant-berkshelf
 - vagrant-hostsupdater
3. Start the vagrant box: `vagrant up`
4. Verify Mesos UI is accessible at [http://vagrant-singularity:5050/](http://vagrant-singularity:5050/), ZK at zk://vagrant-singularity:2181, and mysql at vagrant-singularity:3306 (see Vagrantfile for mysql password)
5. Ensure Singularity has been built: `mvn clean package`
6. Run a database migration to ensure everything is in sync: `java -jar SingularityService/target/SingularityService-*-SNAPSHOT.jar db migrate ./vagrant_singularity.yaml --migrations ./migrations.sql`
7. Start Singularity using the `vagrant_singularity.yaml` config: `java -jar SingularityService/target/SingularityService-*-SNAPSHOT.jar server ./vagrant_singularity.yaml`
