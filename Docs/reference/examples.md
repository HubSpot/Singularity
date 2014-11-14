# Singularity Deploy Examples

These examples assume you have [installed singularity](../install.md).

The services deployed will be a [build](https://github.com/micktwomey/docker-sample-dropwizard-service) of the [Dropwizard getting started example](https://dropwizard.github.io/dropwizard/getting-started.html) and a [simple python service](https://github.com/micktwomey/docker-sample-web-service).

## Creating the Request

All deployments belong to requests, before you can deploy you need to create a request. A request represents your service or scheduled job.

Create a [new request](http://vagrant-singularity:7099/singularity/requests/new) and set the following:

- ID: mesos-dropwizard-service
- Owners: Your email address
- Type: Service

You can also create the request using a JSON HTTP POST:

```json
{
    "id": "mesos-dropwizard-service",
    "owners": [
        "mtwomey@example.com"
    ],
    "daemon": true,
    "rackSensitive": false,
    "loadBalanced": false
}
```

You can POST this JSON (saved in request.json) using curl:

```sh
curl -i -X POST -H "Content-Type: application/json" -d@request.json \
http://vagrant-singularity:7099/singularity/api/requests
```

## Basic Service Using the Mesos Executor

In order to deploy using the default Mesos executor you will need to push the deployment artifacts to an accessible location. In this example they are available in a GitHub [release](https://github.com/micktwomey/docker-sample-dropwizard-service/releases/tag/1.0).

To deploy using the web UI:

- Deploy ID: 1
- Command to execute: ```java -jar helloworld-1.0-SNAPSHOT.jar server example.yml```
- Artifacts:
    - ```https://github.com/micktwomey/docker-sample-dropwizard-service/releases/download/1.0/helloworld-1.0-SNAPSHOT.jar```
    - ```https://github.com/micktwomey/docker-sample-dropwizard-service/releases/download/1.0/example.yml```
- CPUs: 0.1 (in the default Vagrant setup there is only one CPU resource in the slave, to test multiple deployments and scaling we'll use less CPU resources.)

The equivalent JSON which can be used instead of the web UI:

```json
{
    "deploy": {
        "requestId": "mesos-dropwizard-service",
        "id": "1",
        "command": "java -jar helloworld-1.0-SNAPSHOT.jar server example.yml",
        "resources": {
            "cpus": 0.1,
            "memoryMb": 128,
            "numPorts": 0
        },
        "uris": [
            "https://github.com/micktwomey/docker-sample-dropwizard-service/releases/download/1.0/helloworld-1.0-SNAPSHOT.jar", 
            "https://github.com/micktwomey/docker-sample-dropwizard-service/releases/download/1.0/example.yml"
        ],
        "skipHealthchecksOnDeploy": false
    }
}
```

You can POST this JSON (saved in deploy.json) using curl:

```sh
curl -i -X POST -H "Content-Type: application/json" -d@deploy.json \
http://vagrant-singularity:7099/singularity/api/deploys
```

When the task launches nothing may be visible in the Singularity UI at first, this is due to the Mesos executor fetching the artifacts first.

Once the task is running you can go to [http://vagrant-singularity:8080/](http://vagrant-singularity:8080/) to see the JSON response (you can also go to [http://vagrant-singularity:8081/](http://vagrant-singularity:8081/) to see the admin resources).

### Limitations

- Since this container is bound to the ports 8080 and 8081 on the host machine you can't scale it up to more than one per machine.
- Since Singularity isn't allocating the ports you can't use the health check.


## Basic Service using Allocated Ports

Singularity can allocate ports to the service, this allows multiple services to run on the same machine, even when they would ordinarily have port clashes.

When allocating ports Singularity will set PORT0...N environment variables you can use to map your service's ports.

The Dropwizard example exposes a healthcheck on ```http://service:8081/healthcheck```. This can be used by Singularity to determine the healthiness of the service on deploys or when a new task is launched.

The health check uses the first port on the container, so we'll need to map it before the application port.

In this example we'll be using ```java -Ddw.server.applicationConnectors[0].port=$PORT1 -Ddw.server.adminConnectors[0].port=$PORT0  ...``` to map the ports, the Singularity health check uses the first available port so we'll assign admin to port 0 to ensure /healthcheck works.

Make another deploy request:

- Deploy ID: 2
- Command to execute: ```java -Ddw.server.applicationConnectors[0].port=$PORT1 -Ddw.server.adminConnectors[0].port=$PORT0 -jar helloworld-1.0-SNAPSHOT.jar server example.yml```
- Artifacts:
    - ```https://github.com/micktwomey/docker-sample-dropwizard-service/releases/download/1.0/helloworld-1.0-SNAPSHOT.jar```
    - ```https://github.com/micktwomey/docker-sample-dropwizard-service/releases/download/1.0/example.yml```
- CPUs: 0.1 
- Num. ports: 2
- Healthcheck URI: /healthcheck

Or post the following JSON:
```json
{
    "deploy": {
        "requestId": "mesos-dropwizard-service",
        "id": "2",
        "command": "java -Ddw.server.applicationConnectors[0].port=$PORT1 -Ddw.server.adminConnectors[0].port=$PORT0 -jar helloworld-1.0-SNAPSHOT.jar server example.yml",
        "resources": {
            "cpus": 0.1,
            "memoryMb": 128,
            "numPorts": 2
        },
        "uris": [
            "https://github.com/micktwomey/docker-sample-dropwizard-service/releases/download/1.0/helloworld-1.0-SNAPSHOT.jar", 
            "https://github.com/micktwomey/docker-sample-dropwizard-service/releases/download/1.0/example.yml"
        ],
        "skipHealthchecksOnDeploy": false,
        "healthcheckUri": "/healthcheck"
    }
}
```

You can navigate to the running task in the UI and get the two ports. You can then access the service on those ports (for example my service got allocated 31428,31429, so I could then use ```curl http://vagrant-singularity:31429/``` to fetch the hello world JSON).

## Scaling Up

Scaling up is easy, navigate to your [request](http://vagrant-singularity:7099/singularity/request/mesos-dropwizard-service) and click the "Scale" button. Type in a new value (e.g. 3) and wait for the new tasks to run. Since the ports are managed by Singularity you don't have to worry about them clashing.

Side note: You'll notice that each running task returns different ids in the hello world response. This is due to each being an independent process, with separate counters.

## Docker Service with Host Networking

An alternative approach to running in the Mesos executor (or the Singularity executor) is to use [Docker](https://docker.com).

There is a [Docker image](https://registry.hub.docker.com/u/micktwomey/sample-dropwizard-service/) built of the example service which can be used for deployments.

Since we are using host networking the Docker container will bind to the host ports 8080 and 8081 again, so we will need to first scale down the current instances.

To scale down go to your [request](http://vagrant-singularity:7099/singularity/request/mesos-dropwizard-service) and click the "Scale" button again. Enter in 1 to reduce the number of running tasks. You should see two of the tasks move into "Task Cleaning" states.

To deploy this image create another deployment, but use Docker instead of default:

- ID: 3
- Executor Type: Docker
- Docker Image: micktwomey/sample-dropwizard-service:1.0
- CPUs: 0.1

The equivalent JSON:

```json
{
    "deploy": {
        "requestId": "mesos-dropwizard-service",
        "id": "3",
        "containerInfo": {
            "type": "DOCKER",
            "docker": {
                "network": "HOST",
                "image": "micktwomey/sample-dropwizard-service:1.0"
            }
        },
        "resources": {
            "cpus": 0.1,
            "memoryMb": 128,
            "numPorts": 0
        },
        "skipHealthchecksOnDeploy": false
    }
}
```

This will pull down the Docker image from the Docker registry and start the container. The ports will be bound to the Mesos slave host, so the service will be available again at [http://vagrant-singularity:8080/](http://vagrant-singularity:8080/).

## Docker Service with Bridge Networking

Instead of binding to the host network Docker can set up a bridge network and bind the container's ports to the external ports. The container doesn't have to know what the external ports are (or if they are bound at all), so you don't have to pass this configuration into the container.

Unfortunately the Singularity UI doesn't expose the bridge networking details so you have to map them via a JSON request:

```json
{
    "deploy": {
        "requestId": "mesos-dropwizard-service",
        "id": "4",
        "containerInfo": {
            "type": "DOCKER",
            "docker": {
                "network": "BRIDGE",
                "image": "micktwomey/sample-dropwizard-service:1.0",
                "portMappings": [
                    {
                        "containerPortType": "LITERAL",
                        "containerPort": 8081,
                        "hostPortType": "FROM_OFFER",
                        "hostPort": 0,
                        "protocol": "tcp"
                    },
                    {
                        "containerPortType": "LITERAL",
                        "containerPort": 8080,
                        "hostPortType": "FROM_OFFER",
                        "hostPort": 1,
                        "protocol": "tcp"
                    }
                ]
            }
        },
        "resources": {
            "cpus": 0.1,
            "memoryMb": 128,
            "numPorts": 2
        },
        "skipHealthchecksOnDeploy": false,
        "healthcheckUri": "/healthcheck"
    }
}
```

Post this using curl:

```sh
curl -i -X POST -H "Content-Type: application/json" -d@deploy.json  http://vagrant-singularity:7099/singularity/api/deploys
```

You can go back to the [request](http://vagrant-singularity:7099/singularity/request/mesos-dropwizard-service) and watch the task starting.

You can also scale it up in a similar manner, this time you should notice the new tasks starting much faster. This is because the Docker layers are already on the machine, so the Docker pull should be virtually instant.

## Switching Implementations

One big advantage of using Docker is you can change the implementation details of the service easily, without requiring any co-ordination with other deployments or the packages installed on the host machine.

For example, there is a python variation of this service in [micktwomey/docker-sample-web-service](https://github.com/micktwomey/docker-sample-web-service). The image is built on the [Docker Registry](https://registry.hub.docker.com/u/micktwomey/docker-sample-web-service/).

To deploy this service instead change the Docker image being used:

```json
{
    "deploy": {
        "requestId": "mesos-dropwizard-service",
        "id": "5",
        "containerInfo": {
            "type": "DOCKER",
            "docker": {
                "network": "BRIDGE",
                "image": "micktwomey/docker-sample-web-service:latest",
                "portMappings": [
                    {
                        "containerPortType": "LITERAL",
                        "containerPort": 8081,
                        "hostPortType": "FROM_OFFER",
                        "hostPort": 0,
                        "protocol": "tcp"
                    },
                    {
                        "containerPortType": "LITERAL",
                        "containerPort": 8080,
                        "hostPortType": "FROM_OFFER",
                        "hostPort": 1,
                        "protocol": "tcp"
                    }
                ]
            }
        },
        "resources": {
            "cpus": 0.1,
            "memoryMb": 128,
            "numPorts": 2
        },
        "skipHealthchecksOnDeploy": false,
        "healthcheckUri": "/healthcheck"
    }
}
```

# Future Topics

- Load Balancing and Baragon
- Using the Singularity Executor
