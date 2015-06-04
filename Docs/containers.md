## Container Support

Singularity supports containers in a fex different contexts:

###Mesos Containerizer
The default mesos containerizer for processes which sets resource limits/etc. Enabled by adding `mesos` to the `--containerizers` argument when running `mesos-slave`. If this is enabled, commands with no `containerInfo` definition will run under this containerizer.

###Mesos Docker Containerizer
The [built-in docker containerizer](https://mesos.apache.org/documentation/latest/docker-containerizer/) which comes with mesos. This will manage the starting and stopping of your docker container as well as mapping ports, adding environment vairables, and mapping volumes in the container to the mesos sandbox for that task. Enable this containerizer by adding `docker` to the arguments of `--containerizers` when running `mesos-slave`.

To use Singularity with the Docekr containerizer, add a `containerInfo` to the SingularityDeploy object when creating a deploy (without specifying a `customExecutorCmd`). The Singularity deploy object's' `containerInfo` field mirrors the Mesos `containerInfo` definition:

```
{
  "type":"DOCKER",
  "docker": {
    "image": "library/hello-world",
    "privileged": false,
    "network": "BRIDGE", # Can be BRIDGE, HOST, or NONE. Must be BRIDGE if portMappings are set
    "portMappings": [
      {
        "containerPortType": "FROM_OFFER", # FROM_OFFER or LITERAL
        "contianerPort": 0, # If type is FROM_OFFER this is the index of the port assigned by mesos. (ie 0 -> $PORT0)
        "hostPortType": "FROM_OFFER",
        "hostPort": 0
      }
    ]
  },
  "volumes": [
    {
      "containerPath": "/etc/example",
      "hostPath": "/etc/example"
      "mode": "RO" # RO or RW

    }
  ]
}
```

###Singularity Executor

The SingularityExecutor also supports running docker containers, with the added bonuses of the SingularityExecutor like logrotate, S3 upload/download, and thread count monitoring.

To use docker with the Singularity executor, specify the `containerInfo` as seen above in the mesos docker containerizer example, but also include a `customExecutorCmd` field in your deploy point to the SingularityExecutor. (This is the same method of using the SingularityExecutor as for normal tasks, just adding `containerInfo`)

When the SingularityExecutor is given a task with `containerInfo` of type `DOCKER`, it will do the following:
- pull the docker image
- download any other specified artifacts from the deploy
- map all specified environment variables to the container
- assign and map ports and specified volumes
- map the mesos sandbox task directory to `/mnt/mesos/sandbox` in the container
- create and start the container, directing output to the configured `runContext.logFile`
- run a docker stop when receiving a SIGTERM, try to remove the stopped container, and exit with the containers exit code

A few special notes and variables that are set:
- `MESOS_SANDBOX`: The mesos sandbox directory as seen inside the container (generally /mnt/mesos/sandbox)
- `LOG_DIR`: The log directory that SingularityExecutor will use for logrotating/uploading/etc, generally mapped to /mnt/mesos/sandbox/logs in the container
- The docker working directory will be set to the `taskAppDirectory` in the mesos sandbox
- The container name will be the task id
- SingularityExecutor will explicity try to pull the image (ie, must be from a repository rechable by the slave)

Here is an example deploy to get you started:

```
{
  "requestId": "docker-hello-world",
  "id": "1",
  "containerInfo": {
    "type": "DOCKER",
    "docker": {
      "image": "hello-world:latest",
      "privileged": false,
      "network": "HOST",
      "portMappings": []
    }
  },
  "customExecutorCmd": "/usr/local/bin/singularity-executor",
  "resources": {
    "cpus": 1,
    "memoryMb": 1024,
    "numPorts": 1
  },
  "executorData": {
    "cmd": "/hello",
    "embeddedArtifacts": [],
    "externalArtifacts": [],
    "s3Artifacts": [],
    "successfulExitCodes": [
      0
    ],
    "user": "root",
    "extraCmdLineArgs": [],
    "loggingExtraFields": {},
    "maxTaskThreads": 2048
  }
}
```
