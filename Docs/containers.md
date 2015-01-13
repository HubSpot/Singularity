## Container Support

Singularity supports Mesos containers (DOCKERs).  To use containers, you need make sure [your Mesos slave is configured correctly](https://mesos.apache.org/documentation/latest/docker-containerizer/).  

The Singularity deploy object has a `containerInfo` field which mirrors the Mesos `containerInfo` definition.  Here is an example deploy to get you started:

```
{
  "id": "DemoService.Docker",
  "requestId": "DemoService",
  "resources": {
    "cpus": 1,
    "memoryMb": 128,
    "numPorts": 2
  },
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
