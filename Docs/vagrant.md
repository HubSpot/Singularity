# Vagrant

In order to speed up Vagrant provisioning, we publish a "box" to Vagrant Cloud containing Mesos, MySQL, and Zookeeper preinstalled. The `test` and `develop` roles depend on this box.

## Publishing a Singularity base box

To publish a new Singularity base box (i.e. for new versions of Mesos):

1. Provision the `base_image` Vagrant role: `vagrant up base_image`. This will take 10-15 minutes.
2. Verify Mesos, MySQL, ZK are running
3. Export the box: `vagrant box base_image --output singularity-develop-X.Y.Z` (where X.Y.Z is the Mesos version installed). This will take a few minutes.
4. Upload the newly created `singularity-develop-X.Y.Z` to a CDN. The file should be ~2 GB, so this might take awhile.
5. Go to Vagrant Cloud and publish a new box, pointing to the URL of the file you just uploaded
6. Update the `Vagrantfile` to point to this new URL.
