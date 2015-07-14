## Developing With Docker

For developing with Docker, you will need to install [docker](https://docs.docker.com/installation/) and [docker-compose](https://docs.docker.com/compose/#installation-and-set-up). If you are just trying our our example cluster, you can then just run `docker-compose up`, if not, read on!

### `dev`

In the root of this project is a `dev` wrapper script to make developing easier. You can do the following:

```
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
- `hubspot/singularityexecutorslavebase` - A base image for `singularityexecutorslave` that takes care of installing java/logrotate on top of the mesos slave image (not built with maven plugin)

### Logs and Entering Containers

If you are not attached to the docker-compose process, you can check the output of your containers using `docekr logs`. Start by checking `docker ps` to see what containers are running. Generally they will have names like `singularity_(service)`. From there you can run `docker logs (name)` to see th stdout for that container.

Need to see more than stdout? You can also get a shell inside the container and poke around. Once you know the name of your container, you can run `docker exec -it (name) /bin/bash` to get am interactive shell inside the running container.

### Integration Tests

The SingularityServiceIntegrationTests module will run tests on a cluster consisting of a singularity scheduler, sk instance, mesos master, and three mesos slaves. These will run during the `integration-test` lifecycle phase.