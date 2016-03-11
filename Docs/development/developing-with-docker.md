## Setup

For developing or testing out Singularity with Docker, you will need to install [docker](https://docs.docker.com/installation/) and [docker-compose](https://docs.docker.com/compose/#installation-and-set-up).

## Example cluster with Docker Compose

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

*if using [boot2docker](http://boot2docker.io/) or another vm, replace `localhost` with the ip of your vm*

The docker-compose example clsuter will always run off of the most recent release tag.

## Developing With Docker

### `dev`

In the root of this project is a `dev` wrapper script to make developing easier. It will run using images from the current snapshot version. You can do the following:

```
./dev pull           # Get the latest images from docker hub
./dev start          # start mesos clsuter in background
./dev attach         # start mesos cluster and watch output in console
./dev restart        # stop all containers and restart in background
./dev rebuild        # stop all containers, rebuild Singularity and docker images, then start in background
./dev rebuild attach # rebuild and watch output when started
./dev remove         # remove stopped containers
./dev stop           # stop all containers
./dev kill           # kill all containers (ungraceful term)
```

The output from the dev script will give you information about where the SingularityUI can be reached.

### Building new images

Singularity uses the docker-maven-plugin for building its images. There are a few images related to Singularity:

- `hubspot/singularityservice` - The Singularity scheduler itself
- `hubspot/singularityexecutorslave` - A mesos slave with java/logrotate and the custom SingularityExecutor installed
- `hubspot/singularitybase` - A base image for `singularityexecutorslave` that takes care of installing java/logrotate/etc on top of the mesos slave image (not built with maven plugin)

### Logs and Entering Containers

If you are not attached to the docker-compose process, you can check the output of your containers using `docker logs`. Start by checking `docker ps` to see what containers are running. Generally they will have names like `singularity_(process name)`. From there you can run `docker logs (name)` to see the stdout for that container.

Need to see more than stdout? You can also get a shell inside the container and poke around. Once you know the name of your container, you can run `docker exec -it (name) /bin/bash` to get am interactive shell inside the running container.

### Integration Tests

The SingularityServiceIntegrationTests module will run tests on a cluster consisting of a singularity scheduler, zk instance, mesos master, and three mesos slaves. These will run during the `integration-test` lifecycle phase.