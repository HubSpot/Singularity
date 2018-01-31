# Requests and Deploys

Singularity uses the concepts of a `Request` and `Deploy` to keep track of changes to a particular task or set of related tasks.

A `Request` can be thought of as the high level information for a single project or deployable item. For example, the name and owner's email for a single web service, or the name and cron schedule for a single scheduled job.

A `Deploy` can then be thought of as the specific configuration or version of the running code for that deployable item.

To illustrate the differences between and usages of these two concepts, we will walk through an example using Singularity to run a single web service.

## The `TestService` Example

### Creating a `Request`
You have a new web service called `TestService` that you want to run via Singularity. The first thing you need to do is create a `Request` for `TestService`. To create this request, you would `POST` json over http to the Singularity API ([`/api/requests`](../reference/apidocs/api-requests.md#post-apirequests)) or create a request via the new request page in the Singularity UI. Example json:

```json
{
    "id": "TestService",
    "requestType": "SERVICE",
    "owners": ["me@test.com"],
    "instances": 1,
}
```

This `Request` now holds high level information about your web service. Singularity knows it is a long running `SERVICE` named `TestService` and that you want to run `1` instance of this service.

### Creating a `Deploy`

Now you want `TestService` to actually run. To do this, you need to create a `Deploy` for the `TestService` `Request`. This deploy will let Singularity know all of the information necessary to actually build and launch a task. This information includes things like the command to run, environment variables to set, the location of any artifacts to download, or the resources that should be allocated to a task. You would create this deploy by `POST`ing json to the Singularity API's deploy endpoint ([`/api/deploys`](../reference/apidocs/api-deploys.md#post-apideploys)), or creating a new deploy in the Singularity UI. Example json:

```json
{
  "deploy": {
    "requestId":"TestService",
    "id":"5",
    "resources": {
        "cpus":1,
        "memoryMb":128,
        "diskMb": 1024,
        "numPorts":2
    }, 
	"command":"java -Ddw.server.applicationConnectors[0].port=$PORT0 -Ddw.server.adminConnectors[0].port=$PORT1 -jar singularitytest-1.0-SNAPSHOT.jar server example.yml",
	"uris": [
        "https://github.com/HubSpot/singularity-test-service/releases/download/1.0/singularitytest-1.0-SNAPSHOT.jar",
        "https://github.com/HubSpot/singularity-test-service/releases/download/1.0/example.yml"
    ],
    "healthcheck": {
       "uri": "/"
     }
  }
}
```

Posting this to Singularity creates a `PendingDeploy`. Singularity will then try to build and launch tasks using the information provided in the `Deploy` json (i.e. a task with those artifacts that needs 1 cpu, 128MB of memory, etc and is run with that command). Singularity will also know to only build and launch `1` of these tasks based on the number of instances you set in the `Request` earlier. Once that task is launched and Singularity determines it is healthy, the `Deploy` succeeds and the `Deploy` json you provided is now the `ActiveDeploy`.

### A New `Deploy`

Let's say some changes were made to the `TestService` code and you want to run the new version in Singularity, maybe with a bit more memory as well. You would create a new `Deploy` json with information about the new code to run, and updated memory value and `POST` that to the Singularity API ([`/api/deploys`](../reference/apidocs/api-deploys.md#post-apideploys)), or create a new deploy in the Singularity UI.

Singularity sees a new `Deploy` has been started and makes that the `PendingDeploy` for the `TestService` `Request`. Singularity will then try to build and launch `1` new task with the configuration specified in the new `PendingDeploy` (note that the task from the previous `Deploy` is still running and it's settings are unchanged). Once the task is launched and determined to be healthy, that new `Deploy` now becomes the `ActiveDeploy` and the task from the old deploy is shut down.

### Updating the Request

Now, for example, you notice `TestService` is getting more traffic than expected and you want to run `3` instances instead of just `1`. You can `PUT` a new number of instances over http to the Singularity API ([`/api/requests/request/{requestId}/scale`](../reference/apidocs/api-requests.md#put-apirequestsrequestrequestidscale)), or click `Scale` on the Singularity UI request page and enter a new number of instances to update our number of instances. Example json:

```json
{
    "id": "TestService",
    "requestType": "SERVICE",
    "owners": ["me@test.com"],
    "instances": 3,
}
```

Singularity sees this update to the request and will try to build and launch `2` additional tasks so that there are `3` total instances running. The configuration/resources/command/etc for these new tasks will be determined from the current `ActiveDeploy`. In other words, there is no need to re-deploy `TestService` to scale the number of instances up or down. This information is saved separate from the more detailed information in the deploy. If you were to create another new deploy after scaling, Singularity would then build and launch `3` new tasks for that new deploy.
