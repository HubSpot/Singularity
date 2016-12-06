# Singularity Deploy Examples

- [Creating A Request](#creating-a-request)
- [Basic Service Using the Mesos Executor](#basic-service-using-the-mesos-executor)
- [Basic Service Using Allocated Ports](#basic-service-using-allocated-ports)
- [Basic Load Balanced Service with Allocated Ports](#basic-load-balanced-service-with-allocated-ports)
- [Scaling Up Services](#scaling-up)
- [Docker Service with Host Networking](#docker-service-with-host-networking)
- [Docker Service with Bridge Networking](#docker-service-with-bridge-networking)
- [Load Balanced Docker Service Using The SingularityExecutor](#load-balanced-docker-service-using-the-singularityexecutor)

These examples assume you have [installed Singularity](install.md).

The services deployed will be a [build](https://github.com/HubSpot/singularity-test-service) of a sample Dropwizard service. It has two enpoints `/hello` and `/environment`.

*For this walkthrough we will assume you are using the [docker-compose example cluster](../getting-started/try-it-out.md) and that Singularity is running at `http://localhost:7099/singularity`. For your own case you can replace `localhost` with whatever host Singularity is running on in your setup. (e.g. if using `boot2docker` you would use the vm's ip of 192.168.59.103)*

## Creating A Request

All deployments belong to [requests](../about/requests-and-deploys.md). Before you can deploy you need to create a request. A request represents your service or scheduled job.

Create a new request on the new request page ([`/singularity/requests/new`](http://localhost:7099/singularity/requests/new)) in the ui, and set the following:

- ID: singularity-test-service
- Owners: Your email address
- Type: Service

You can also create the request using a JSON HTTP POST:

```json
{
    "id": "singularity-test-service",
    "owners": [
        "you@example.com"
    ],
    "requestType": "SERVICE",
    "rackSensitive": false,
    "loadBalanced": false
}
```

You can POST this JSON (saved in request.json) using curl:

```sh
curl -i -X POST -H "Content-Type: application/json" -d@request.json \
http://locahost:7099/singularity/api/requests
```

## Basic Service Using the Mesos Executor

In order to deploy using the default Mesos executor you will need to push the deployment artifacts to an accessible location. In this example they are available in a GitHub [release](https://github.com/HubSpot/singularity-test-service/releases/tag/1.0).

To deploy using the web UI:

- Deploy ID: 1
- Command to execute: ```java -jar singularitytest-1.0-SNAPSHOT.jar server example.yml```
- Artifacts:
    - ```https://github.com/HubSpot/singularity-test-service/releases/download/1.0/singularitytest-1.0-SNAPSHOT.jar```
    - ```https://github.com/HubSpot/singularity-test-service/releases/download/1.0/example.yml```
- CPUs: 0.1 (in the default docker setup there is only one CPU resource in the slave, to test multiple deployments and scaling we'll use less CPU resources.)

The equivalent JSON which can be used instead of the web UI:

```json
{
    "deploy": {
        "requestId": "singularity-test-service",
        "id": "1",
        "command": "java -jar singularitytest-1.0-SNAPSHOT.jar server example.yml",
        "resources": {
            "cpus": 0.1,
            "memoryMb": 128,
            "numPorts": 2
        },
        "uris": [
            "https://github.com/HubSpot/singularity-test-service/releases/download/1.0/singularitytest-1.0-SNAPSHOT.jar", 
            "https://github.com/HubSpot/singularity-test-service/releases/download/1.0/example.yml"
        ]
    }
}
```

You can POST this JSON (saved in deploy.json) using curl:

```sh
curl -i -X POST -H "Content-Type: application/json" -d@deploy.json \
http://localhost:7099/singularity/api/deploys
```

When the task launches nothing may be visible in the Singularity UI at first, this is due to the Mesos executor fetching the artifacts first.

Once the task is running you can go to [http://localhost:7099/singularity/requests/singularity-test-service](http://localhost:7099/singularity/requests/singularity-test-service) to see the currently running tasks and status of the request.

### Limitations

- Since this container is bound to the ports 8080 and 8081 on the host machine you can't scale it up to more than one per machine. Depending on your setup, you may already see port in use errors when starting the app, as those ports are commonly used by other processes.


## Basic Service Using Dynamically Allocated Ports

Singularity can allocate ports to the service, this allows multiple services to run on the same machine, even when they would ordinarily have port clashes.

When allocating ports Singularity will set PORT0...N environment variables you can use to map your service's ports.

The Dropwizard example exposes a healthcheck on ```http://(host):8081/healthcheck```. This can be used by Singularity to determine the healthiness of the service on deploys or when a new task is launched.

The health check by default uses the first port on the container, so we'll need to map it before the application port.

In this example we'll be using ```java -Ddw.server.applicationConnectors[0].port=$PORT1 -Ddw.server.adminConnectors[0].port=$PORT0  ...``` to map the ports, the Singularity health check uses the first available port so we'll assign admin to port 0 to ensure /healthcheck works.

Make another deploy request:

- Deploy ID: 2
- Command to execute: ```java -Ddw.server.applicationConnectors[0].port=$PORT1 -Ddw.server.adminConnectors[0].port=$PORT0 -jar singularitytest-1.0-SNAPSHOT.jar server example.yml```
- Artifacts:
    - ```https://github.com/HubSpot/singularity-test-service/releases/download/1.0/singularitytest-1.0-SNAPSHOT.jar```
    - ```https://github.com/HubSpot/singularity-test-service/releases/download/1.0/example.yml```
- CPUs: 0.1 
- Num. ports: 2
- Healthcheck URI: /healthcheck

Or post the following JSON:
```json
{
    "deploy": {
        "requestId": "singularity-test-service",
        "id": "2",
        "command": "java -Ddw.server.applicationConnectors[0].port=$PORT1 -Ddw.server.adminConnectors[0].port=$PORT0 -jar singularitytest-1.0-SNAPSHOT.jar server example.yml",
        "resources": {
            "cpus": 0.1,
            "memoryMb": 128,
            "numPorts": 2
        },
        "uris": [
            "https://github.com/HubSpot/singularity-test-service/releases/download/1.0/singularitytest-1.0-SNAPSHOT.jar", 
            "https://github.com/HubSpot/singularity-test-service/releases/download/1.0/example.yml"
        ],
        "healthcheck": {
          "uri": "/healthcheck"
        }
    }
}
```

You can navigate to the running task in the UI and get the two ports. You can then access the service on those ports (for example my service got allocated 31428,31429, so I could then use ```curl http://localhost:31429/``` to fetch the hello world JSON).

## Basic Load Balanced Service with Allocated Ports

If Singularity is [configured with a load balancer api](../development/load-balancer-integration.md) like [Baragon](https://github.com/HubSpot/Baragon), you can also have Singularity keep your load balancer up to date. When a task is started and healthy, Singularity will notify the load balacner api of the new service and the port that it is running on.

Because our previous request is already running, you cannot update our non-load-balanced service to be load balanced. Instead, create a new request for a load balanced version of our service. This can be done by POSTing json to the request endpoint similar to the following (note the `"loadBalanced": true`).

```json
{
    "id": "singularity-test-load-balanced-service",
    "owners": [
        "you@example.com"
    ],
    "requestType": "SERVICE",
    "rackSensitive": false,
    "loadBalanced": true
}
```

You can POST this JSON (saved in request.json) using curl:

```sh
curl -i -X POST -H "Content-Type: application/json" -d@request.json \
http://locahost:7099/singularity/api/requests
```

We will need to add some information for the load balancer api to our JSON:

```
"serviceBasePath":"/",
"loadBalancerGroups":["test"]
```

This allows Singularity to tell the load balancer api what groups/clusters the service should be available on, and what path on that cluster belongs to this service.

Make another deploy request by posting the following JSON:
```json
{
    "deploy": {
        "requestId": "singularity-test-load-balanced-service",
        "id": "3",
        "command": "java -Ddw.server.applicationConnectors[0].port=$PORT1 -Ddw.server.adminConnectors[0].port=$PORT0 -jar singularitytest-1.0-SNAPSHOT.jar server example.yml",
        "resources": {
            "cpus": 0.1,
            "memoryMb": 128,
            "numPorts": 2
        },
        "uris": [
            "https://github.com/HubSpot/singularity-test-service/releases/download/1.0/singularitytest-1.0-SNAPSHOT.jar", 
            "https://github.com/HubSpot/singularity-test-service/releases/download/1.0/example.yml"
        ],
        "skipHealthchecksOnDeploy": false,
        "healthcheck": {
          "uri": "/healthcheck"
        },
        "serviceBasePath":"/",
        "loadBalancerGroups":["test"]
    }
}
```

After the task becomes healthy you will also be able to see that the service was successfully [registered with the load balancer api](http://localhost:8080/baragon/v2/ui/services). 

## Scaling Up

Scaling up is easy, navigate to your [request](http://localhost:7099/singularity/request/singularity-test-service) and click the "Scale" button. Type in a new value (e.g. 3) and wait for the new tasks to run. Since the ports are managed by Singularity you don't have to worry about them clashing.

Side note: You'll notice that each running task returns different ids in the hello world response. This is due to each being an independent process, with separate counters.

## Docker Service with Host Networking

An alternative approach to running in the Mesos executor (or the Singularity executor) is to use [Docker](https://docker.com).

There is a [Docker image](https://hub.docker.com/r/hubspot/singularity-test-service/) built of the example service which can be used for deployments.

Since we are using host networking the Docker container will bind to the host ports 8080 and 8081 again, so we will need to first scale down the current instances.

To scale down go to your [request](http://localhost:7099/singularity/request/singularity-test-service) and click the "Scale" button again. Enter in 1 to reduce the number of running tasks. You should see two of the tasks move into "Task Cleaning" states.

To deploy this image create another deployment, but use Docker instead of default:

- ID: 3
- Executor Type: Docker
- Docker Image: hubspot/singularity-test-service:1.0
- CPUs: 0.1

The equivalent JSON:

```json
{
    "deploy": {
        "requestId": "singularity-test-service",
        "id": "3",
        "containerInfo": {
            "type": "DOCKER",
            "docker": {
                "network": "HOST",
                "image": "hubspot/singularity-test-service:1.0"
            }
        },
        "resources": {
            "cpus": 0.1,
            "memoryMb": 128,
            "numPorts": 0
        }
    }
}
```

This will pull down the Docker image from the Docker registry and start the container. The ports will be bound to the Mesos slave host, so the service will be available again at [http://localhost:8080/](http://localhost:8080/). The command to run is already set in the docker image, so in this case it is not set in the deploy. Any command set in the deploy json will override the command for the docker container (i.e CMD in Dockerfile)

## Docker Service with Bridge Networking and Dynamically Allocated Ports

Instead of binding to the host network Docker can set up a bridge network and bind the container's ports to the external ports. The container doesn't have to know what the external ports are (or if they are bound at all), so you don't have to pass this configuration into the container.

You could specify port mappings in the Singularity UI or POST JSON like the following:

```json
{
    "deploy": {
        "requestId": "singularity-test-service",
        "id": "4",
        "containerInfo": {
            "type": "DOCKER",
            "docker": {
                "network": "BRIDGE",
                "image": "hubspot/singularity-test-service:1.0",
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
        "healthcheck": {
          "uri": "/healthcheck"
        }
    }
}
```

Post this using curl:

```sh
curl -i -X POST -H "Content-Type: application/json" -d@deploy.json  http://localhost:7099/singularity/api/deploys
```

The `FROM_OFFER` specification and `hostPort` of `0` lets Singularity know to use the dynamically allocated port with index `0` (i.e. the first port) as the `hostPort`.

You can go back to the [request](http://localhost:7099/singularity/request/singularity-test-service) and watch the task starting.

You can also scale it up in a similar manner, this time you should notice the new tasks starting much faster. This is because the Docker layers are already on the machine, so the Docker pull should be nearly instant.

## Load Balanced Service Using The SingularityExecutor

As we saw above we can add the `loadBalancerGroups` and `serviceBasePath` fields to our deploy and have our service be load balanced.

Now, we also want to add in the SingularityExecutor, Singularity's custom executor. We can use the SingularityExecutor by adding a `customExecutorCmd` (where to find the executor code) and `executorData` (data to pass to our custom executor) in the deploy JSON:

```json
{
    "deploy": {
        "requestId": "singularity-test-load-balanced-service",
        "id": "4",
        "resources": {
            "cpus": 0.1,
            "memoryMb": 128,
            "numPorts": 2
        },
        "healthcheck": {
          "uri": "/healthcheck"
        },
        "serviceBasePath":"/",
        "loadBalancerGroups":["test"],
        "customExecutorCmd": "/usr/local/bin/singularity-executor",
        "executorData": {
            "cmd":"java -Ddw.server.applicationConnectors[0].port=$PORT1 -Ddw.server.adminConnectors[0].port=$PORT0 -jar singularitytest-1.0-SNAPSHOT.jar server example.yml",
            "embeddedArtifacts":[],
            "externalArtifacts": [
                {
                    "name": "singularitytest-1.0-SNAPSHOT.jar",
                    "filename": "singularitytest-1.0-SNAPSHOT.jar",
                    "url": "https://github.com/HubSpot/singularity-test-service/releases/download/1.0/singularitytest-1.0-SNAPSHOT.jar"
                },
                {
                    "name": "example.yml",
                    "filename": "example.yml",
                    "url": "https://github.com/HubSpot/singularity-test-service/releases/download/1.0/example.yml"
                }
            ],
            "s3Artifacts": [],
            "successfulExitCodes": [0],
            "user": "root",
            "extraCmdLineArgs": [],
            "loggingExtraFields": {},
            "maxTaskThreads": 2048
        }
    }
}
```

`POST`ing this to Singularity we now have a docker container with mapped ports connected to a load balancer and running via the SingularityExecutor.

Note that the SingularityExecutor [also has docker support](../reference/container-options.md) (separate form the mesos docker containerizer). By specifying a `containerInfo` section, the SingularityExecutor will manage the lifecycle of your container.
