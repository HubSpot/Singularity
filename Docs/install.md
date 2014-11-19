## Install Singularity with Vagrant
Follow the instructions to create a virtual machine that runs Singularity on top of a Mesos cluster. All Mesos components (Mesos master and slave), all Singularity components (Singularity master, Singularity Executor, Singularity UI, S3Archiver, etc.) as well as their dependencies (Zookeper, MySQL) will run in the same virtual machine.

This setup is only meant for testing purposes; to give you the possibility to quickly take a look and experiment with the available tools, UIs and APIs.

The setup steps have been tested on mac computers running MAC OS X 10.9.x but they should as well work on any recent Linux distribution.

Install [Vagrant](http://www.vagrantup.com/downloads.html)

Install [Virtualbox](https://www.virtualbox.org/wiki/Downloads)

Open a command shell and run the following command to install the `vagrant-hostsupdater` plugin:

```bash
vagrant plugin install vagrant-hostsupdater
```

Clone Singularity from *github.com* in your preferred directory and go into the *vagrant* directory inside the cloned project:

```bash
cd my_home/tests
git clone git@github.com:HubSpot/Singularity.git
cd Singularity/vagrant
ls
```

Look for the provided *Vagrantfile* that contains the required vagrant commands for setting up a *VirtualBox* VM with all required software. The utilized vagrant provisioner for performing the installation is *chef-solo* along with the *Berkshelf* plugin for handling the required chef recipe. The provided *Berksfile* contains information about the *singularity* chef recipe that builds the VM. To start building the VM run the following command:

```bash
vagrant up
```

This command will first setup and then bring up the virtual machine. The first time you run this, you should be patient because it needs to download a basic Linux image and then build Singularity. When this is done the first time, every other time that you run *vagrant up*, it will take only a few seconds to boot the virtual machine up.

During the installation your local machine hosts file is configured with the VM IP (so you can access it as *vagrant-singularity*) and you will be asked to provide your password.

When vagrant command finishes check that everything has been installed successfully executing the following steps:

First verify that Zookeeper is running by logging into the virtual machine and using the zookeeper command line tool to connect to the zookeeper server and list the available nodes:
```bash
vagrant ssh
sudo /usr/share/zookeeper/bin/zkCli.sh -server localhost:2181

# When connected execute the following command to list the root nodes:
ls /

# You should see the following listing:
# [singularity, mesos, zookeeper]

# type 'quit' to exit zookeper console
```

Then verify that the mesos cluster is running and the Mesos UI is accessible at:

[http://vagrant-singularity:5050/](http://vagrant-singularity:5050/)

Verify that mysql server is running:

```bash
mysql -u root -p

# specify *mesos7mysql* as password

#then check if singularity database has been created:

mysql> show databases;

# You should something like the following:
+--------------------+
| Database           |
+--------------------+
| information_schema |
| mysql              |
| performance_schema |
| singularity        |
+--------------------+

# type 'exit' to exit mysql client console
```

Verify that Singularity is running:

[http://vagrant-singularity:7099/singularity](http://vagrant-singularity:7099/singularity)

If everything went well you will see the following screen:
![Singularity UI first run](images/SingularityUI_First_Run.png)

Enter your username to let Singularity populate a personalized dashboard and go to [Deploy Examples](reference/examples.md) to find out how to deploy some test projects.

At a later time you can update the VM installed packages using the latest *singularity* chef recipe by running:
```bash
vagrant reload --provision
```
