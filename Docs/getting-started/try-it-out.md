### Try It Out!

If you want to give Singularity a try, you can install [docker](https://docs.docker.com/installation/) and [docker-compose](https://docs.docker.com/compose/#installation-and-set-up) to run our example cluster.

Run `docker-compose pull` first to get all of the needed images. *Note: This may take a few minutes*

Then simply run `docker-compose up` and it will start containers for...
- mesos master
- mesos slave (docker/mesos containerizers enabled)
- zookeeper
- Singularity
- [Baragon Service](https://github.com/HubSpot/Baragon) for load balancer management
- [Baragon Agent](https://github.com/HubSpot/Baragon) + Nginx as a load balancer

...and the following UIs will be available:
- Singularity UI => [http://localhost:7099/singularity](http://localhost:7099/singularity)
- Baragon UI => [http://localhost:8080/baragon/v2/ui](http://localhost:8080/baragon/v2/ui)

*if using [boot2docker](http://boot2docker.io/) or another vm, replace localhost with the ip of your vm*