### Try It Out!

If you want to give Singularity a try, you can install [docker](https://docs.docker.com/installation/) and [docker-compose](https://docs.docker.com/compose/#installation-and-set-up) to run our example cluster.

**Note:** The Docker development setup is currently not compatible with Docker for Mac due to its usage of `HOST` network mode. You can instead run Singularity via `docker-machine` using the following commands:

Create a new docker machine: `docker-machine create --driver=virtualbox singularity`
Set the docker env to the new machine: `eval $(docker-machine env singularity)`
Get the IP for the machine. Use this instead of `localhost` for the UI addresses: `docker-machine ip singularity`

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
