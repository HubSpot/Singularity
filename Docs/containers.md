## Container Support

Singularity now has basic support for Mesos containers, as of 0.20.0!  First you need to [set up your Mesos slave](https://mesos.apache.org/documentation/latest/docker-containerizer/) to support containers.  The Singularity deploy object has a `containerInfo` field which mirrors the Mesos `containerInfo` definition.  Here is an example deploy to get you started:

```
{
  "id": "DemoService.Docker",
  "requestId": "DemoService",
  "resources": {
    "cpus": 1,
    "memoryMb": 128,
    "numPorts": 2
  },
  "serviceBasePath": "/demo",
  "healthcheckUri": "/health",
  "healthcheckTimeoutSeconds": 300,
  "containerInfo": {
    "type": "DOCKER",
    "docker": {
      "image": "my-registry.mydomain.com/demo-server:latest"
    }
  },
  "env": {
    "FOO": "bar"
  }
}
```
