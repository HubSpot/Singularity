Cluster Coordinator
===================

The SingularityClusterCoordinator module is a proxy service that implements the same api as SingularityService. It is meant to stand in front of a multiple mesos cluster setup as a way to consolidate the information from those clusters/data centers and simplify communication with them.

### General Concepts

When the proxy receives a request, it searches for key pieces of information to find out where it should route the request. Write operations or GETs for a specific task/request need to be routed to an individual cluster. The coordinator can route requests based on:

- Request ID (and therefore task ID since it contains the request ID)
- Slave/Agent ID or hostname
- Request group ID
- The `dataCenter` field on the SingularityRequest object (when creating new requests)

For other GET requests that gather lists of data, such as the `/slaves` or `/tasks` endpoints, will aggregate the data from all configured clusters.

Beacuse of this, for most general operations, the SingularityUI or SingularityClient can be pointed at the proxy to seamlessly interact with multiple installations of Singularity.

Currently, the following endpoints are not implemented on the coordinator and will throw a `NotImpplementedException`:

- Disasters
- Priority freeze
- Status/State

### Configuration

The following fields inform the operation of the proxy:

| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| `dataCenters` | |A list of possible Singularity clusters in the format show in `DataCenter` below. The first of these specified is considered the 'default' data center| `List<DataCenter>` |
| `errorOnDataCenterNotSpecified` |`false`| If `true` return a 500 when the datacenter for a new request can't be determined, if `false` route this request to the default data center | `boolean` |

The following fields inform defaults or behavior of the UI (which can be served by the proxy without contacting any SingularityService instance). Ideally these should match the values set for `SingularityService`

| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| `uiConfiguration` | | mimics the settings in the [SingularityService UI Configuration](../reference/configuration.md#ui-configuration) | `UIConfiguration` |
| `defaultCpus` | 1 | Number of CPUs to request for a task if none are specified | `int` |
| `defaultMemory` | 64 | MB of memory to request for a task if none is specified | `int` |
| `slaveHttpPort` | 5051 | The port to talk to slaves on | `int` |
| `bounceExpirationMinutes` | 60 | Expire a bounce after this many minutes if an expiration is not provided in the request to bounce | `int` |
| `healthcheckIntervalSeconds` | 5 | Default amount of time to wait in between attempting task healthchecks | `int` |
| `healthcheckTimeoutSeconds` | 5 | Default amount of time to wait for healthchecks to return before considering them failed | `int` | 
| `healthcheckMaxRetries` | | Default max number of time to retry a failed healthcheck for a task before considering the task to be unhealthy | `int` |
| `startupTimeoutSeconds` | 45 | If a healthchecked task has not responded with a valid http response in `startupTimeoutSeconds` consider it unhealthy | `int` |
| `commonHostnameSuffixToOmit` | null | If specified, will remove this hostname suffix from all taskIds | string |
| `warnIfScheduledJobIsRunningPastNextRunPct` | 200 | Warn if a scheduled job has run this much past its next scheduled run time (e.g. 200 => ran through next two run times) | `int` |
| `loadBalancingEnabled`| `false` | Display load balancing related messaging in the ui if load balancing is enabled | `boolean` |


#### `DataCenter`

| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| name | | The name this data center will be referred to by. This will be the value you specify in `dataCenter` on the SingularityRequest object to create a request in that data center | `String` |
| hosts || A list of hostnames for SingularityService instances in this cluster | `List<String>` |
| scheme | `http` | `http` or `https` | `String` |
| contextPath | | Context path for the SingularityService instances in this cluster (e.g. `singularity/v2/api` | `String` |
| clientCredentials | `Optional.absent()` | Default credentials used to initially load data from this data center, specified as a `Map<String, String>` with fields of `headerName` and `token`| `Optional<SingularityClientCredentials>` |
