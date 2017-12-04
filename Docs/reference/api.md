# Singularity REST API

Version: 0.19.0-SNAPSHOT

Endpoints:
- [`/api/racks`](#endpoint-/api/racks) - Manages Singularity racks.
- [`/api/webhooks`](#endpoint-/api/webhooks) - Manages Singularity webhooks.
- [`/api/priority`](#endpoint-/api/priority) - Manages whether or not to schedule tasks based on their priority levels.
- [`/api/logs`](#endpoint-/api/logs) - Manages Singularity task logs stored in S3.
- [`/api/track`](#endpoint-/api/track) - Find a task by taskId or runId
- [`/api/test`](#endpoint-/api/test) - Misc testing endpoints.
- [`/api/history`](#endpoint-/api/history) - Manages historical data for tasks, requests, and deploys.
- [`/api/state`](#endpoint-/api/state) - Provides information about the current state of Singularity.
- [`/api/tasks`](#endpoint-/api/tasks) - Manages Singularity tasks.
- [`/api/sandbox`](#endpoint-/api/sandbox) - Provides a proxy to Mesos sandboxes.
- [`/api/slaves`](#endpoint-/api/slaves) - Manages Singularity slaves.
- [`/api/disasters`](#endpoint-/api/disasters) - Manages Singularity Deploys for existing requests
- [`/api/requests`](#endpoint-/api/requests) - Manages Singularity Requests, the parent object for any deployed task
- [`/api/groups`](#endpoint-/api/groups) - Manages Singularity Request Groups, which are collections of one or more Singularity Requests
- [`/api/deploys`](#endpoint-/api/deploys) - Manages Singularity Deploys for existing requests

Models:
- [`EmbeddedArtifact`](#model-EmbeddedArtifact)
- [`ExecutorData`](#model-ExecutorData)
- [`ExternalArtifact`](#model-ExternalArtifact)
- [`HealthcheckOptions`](#model-HealthcheckOptions)
- [`LoadBalancerRequestId`](#model-LoadBalancerRequestId)
- [`MesosFileChunkObject`](#model-MesosFileChunkObject)
- [`MesosResourcesObject`](#model-MesosResourcesObject)
- [`MesosTaskStatisticsObject`](#model-MesosTaskStatisticsObject)
- [`Resources`](#model-Resources)
- [`S3Artifact`](#model-S3Artifact)
- [`S3ArtifactSignature`](#model-S3ArtifactSignature)
- [`Set`](#model-Set)
- [`SingularityBounceRequest`](#model-SingularityBounceRequest)
- [`SingularityContainerInfo`](#model-SingularityContainerInfo)
- [`SingularityDeleteRequestRequest`](#model-SingularityDeleteRequestRequest)
- [`SingularityDeploy`](#model-SingularityDeploy)
- [`SingularityDeployFailure`](#model-SingularityDeployFailure)
- [`SingularityDeployHistory`](#model-SingularityDeployHistory)
- [`SingularityDeployMarker`](#model-SingularityDeployMarker)
- [`SingularityDeployProgress`](#model-SingularityDeployProgress)
- [`SingularityDeployRequest`](#model-SingularityDeployRequest)
- [`SingularityDeployResult`](#model-SingularityDeployResult)
- [`SingularityDeployStatistics`](#model-SingularityDeployStatistics)
- [`SingularityDeployUpdate`](#model-SingularityDeployUpdate)
- [`SingularityDisabledAction`](#model-SingularityDisabledAction)
- [`SingularityDisabledActionRequest`](#model-SingularityDisabledActionRequest)
- [`SingularityDisaster`](#model-SingularityDisaster)
- [`SingularityDisasterDataPoint`](#model-SingularityDisasterDataPoint)
- [`SingularityDisastersData`](#model-SingularityDisastersData)
- [`SingularityDockerInfo`](#model-SingularityDockerInfo)
- [`SingularityDockerParameter`](#model-SingularityDockerParameter)
- [`SingularityDockerPortMapping`](#model-SingularityDockerPortMapping)
- [`SingularityExitCooldownRequest`](#model-SingularityExitCooldownRequest)
- [`SingularityExpiringBounce`](#model-SingularityExpiringBounce)
- [`SingularityExpiringMachineState`](#model-SingularityExpiringMachineState)
- [`SingularityExpiringPause`](#model-SingularityExpiringPause)
- [`SingularityExpiringScale`](#model-SingularityExpiringScale)
- [`SingularityExpiringSkipHealthchecks`](#model-SingularityExpiringSkipHealthchecks)
- [`SingularityHostState`](#model-SingularityHostState)
- [`SingularityLoadBalancerUpdate`](#model-SingularityLoadBalancerUpdate)
- [`SingularityMachineChangeRequest`](#model-SingularityMachineChangeRequest)
- [`SingularityMachineStateHistoryUpdate`](#model-SingularityMachineStateHistoryUpdate)
- [`SingularityMesosArtifact`](#model-SingularityMesosArtifact)
- [`SingularityMesosTaskLabel`](#model-SingularityMesosTaskLabel)
- [`SingularityPauseRequest`](#model-SingularityPauseRequest)
- [`SingularityPendingDeploy`](#model-SingularityPendingDeploy)
- [`SingularityPendingRequest`](#model-SingularityPendingRequest)
- [`SingularityPendingTask`](#model-SingularityPendingTask)
- [`SingularityPendingTaskId`](#model-SingularityPendingTaskId)
- [`SingularityPriorityFreeze`](#model-SingularityPriorityFreeze)
- [`SingularityPriorityFreezeParent`](#model-SingularityPriorityFreezeParent)
- [`SingularityRack`](#model-SingularityRack)
- [`SingularityRequest`](#model-SingularityRequest)
- [`SingularityRequestCleanup`](#model-SingularityRequestCleanup)
- [`SingularityRequestDeployState`](#model-SingularityRequestDeployState)
- [`SingularityRequestGroup`](#model-SingularityRequestGroup)
- [`SingularityRequestHistory`](#model-SingularityRequestHistory)
- [`SingularityRequestParent`](#model-SingularityRequestParent)
- [`SingularityRunNowRequest`](#model-SingularityRunNowRequest)
- [`SingularityS3LogMetadata`](#model-SingularityS3LogMetadata)
- [`SingularityS3SearchRequest`](#model-SingularityS3SearchRequest)
- [`SingularityS3SearchResult`](#model-SingularityS3SearchResult)
- [`SingularitySandbox`](#model-SingularitySandbox)
- [`SingularitySandboxFile`](#model-SingularitySandboxFile)
- [`SingularityScaleRequest`](#model-SingularityScaleRequest)
- [`SingularityShellCommand`](#model-SingularityShellCommand)
- [`SingularitySkipHealthchecksRequest`](#model-SingularitySkipHealthchecksRequest)
- [`SingularitySlave`](#model-SingularitySlave)
- [`SingularityState`](#model-SingularityState)
- [`SingularityTask`](#model-SingularityTask)
- [`SingularityTaskCleanup`](#model-SingularityTaskCleanup)
- [`SingularityTaskCredits`](#model-SingularityTaskCredits)
- [`SingularityTaskHealthcheckResult`](#model-SingularityTaskHealthcheckResult)
- [`SingularityTaskHistory`](#model-SingularityTaskHistory)
- [`SingularityTaskHistoryUpdate`](#model-SingularityTaskHistoryUpdate)
- [`SingularityTaskId`](#model-SingularityTaskId)
- [`SingularityTaskIdHistory`](#model-SingularityTaskIdHistory)
- [`SingularityTaskIdsByStatus`](#model-SingularityTaskIdsByStatus)
- [`SingularityTaskMetadata`](#model-SingularityTaskMetadata)
- [`SingularityTaskMetadataRequest`](#model-SingularityTaskMetadataRequest)
- [`SingularityTaskReconciliationStatistics`](#model-SingularityTaskReconciliationStatistics)
- [`SingularityTaskRequest`](#model-SingularityTaskRequest)
- [`SingularityTaskShellCommandHistory`](#model-SingularityTaskShellCommandHistory)
- [`SingularityTaskShellCommandRequest`](#model-SingularityTaskShellCommandRequest)
- [`SingularityTaskShellCommandRequestId`](#model-SingularityTaskShellCommandRequestId)
- [`SingularityTaskShellCommandUpdate`](#model-SingularityTaskShellCommandUpdate)
- [`SingularityTaskState`](#model-SingularityTaskState)
- [`SingularityUnpauseRequest`](#model-SingularityUnpauseRequest)
- [`SingularityUpdatePendingDeployRequest`](#model-SingularityUpdatePendingDeployRequest)
- [`SingularityUser`](#model-SingularityUser)
- [`SingularityVolume`](#model-SingularityVolume)
- [`SingularityWebhook`](#model-SingularityWebhook)
- [`SingularityWebhookSummary`](#model-SingularityWebhookSummary)

- - -

## Endpoints
### <a name="endpoint-/api/racks"></a> /api/racks
#### Overview
Manages Singularity racks.

#### **POST** `/api/racks/rack/{rackId}/freeze`

Freeze a specific rack


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Rack ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityMachineChangeRequest](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/racks/rack/{rackId}/expiring`

Delete any expiring machine state changes for this rack


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Active slaveId | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/racks/rack/{rackId}/decommission`

Begin decommissioning a specific active rack


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Active rack ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityMachineChangeRequest](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/racks/rack/{rackId}/activate`

Activate a decomissioning rack, canceling decomission without erasing history


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Active rackId | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityMachineChangeRequest](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/racks/rack/{rackId}`

Retrieve the history of a given rack


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Rack ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityMachineStateHistoryUpdate]](#model-SingularityMachineStateHistoryUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/racks/rack/{rackId}`

Remove a known rack, erasing history. This operation will cancel decommissioning of racks


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Rack ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/racks/expiring`

Get all expiring state changes for all racks


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityExpiringMachineState]](#model-SingularityExpiringMachineState)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/racks/`

Retrieve the list of all known racks, optionally filtering by a particular state


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| state | false | Optionally specify a particular state to filter racks by | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityRack]](#model-SingularityRack)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="endpoint-/api/webhooks"></a> /api/webhooks
#### Overview
Manages Singularity webhooks.

#### **DELETE** `/api/webhooks/{webhookId}`

Delete a specific webhook.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
string


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks/task/{webhookId}`

Retrieve a list of queued task updates for a specific webhook.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityTaskHistoryUpdate]](#model-SingularityTaskHistoryUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks/task`

Retrieve a list of queued task updates for a specific webhook.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | false |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityTaskHistoryUpdate]](#model-SingularityTaskHistoryUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks/summary`

Retrieve a summary of each active webhook


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityWebhookSummary]](#model-SingularityWebhookSummary)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks/request/{webhookId}`

Retrieve a list of queued request updates for a specific webhook.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityRequestHistory]](#model-SingularityRequestHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks/request`

Retrieve a list of queued request updates for a specific webhook.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | false |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityRequestHistory]](#model-SingularityRequestHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks/deploy/{webhookId}`

Retrieve a list of queued deploy updates for a specific webhook.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityDeployUpdate]](#model-SingularityDeployUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks/deploy`

Retrieve a list of queued deploy updates for a specific webhook.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | false |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityDeployUpdate]](#model-SingularityDeployUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/webhooks`

Add a new webhook.


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityWebhook](#model-linkType)</a> |

###### Response
string


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks`

Retrieve a list of active webhooks.


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityWebhook]](#model-SingularityWebhook)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/webhooks`

Delete a specific webhook.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | false |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
string


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="endpoint-/api/priority"></a> /api/priority
#### Overview
Manages whether or not to schedule tasks based on their priority levels.

#### **DELETE** `/api/priority/freeze`

Stops the active priority freeze.


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 202    | The active priority freeze was deleted. | - |
| 400    | There was no active priority freeze to delete. | - |


- - -
#### **POST** `/api/priority/freeze`

Stop scheduling tasks below a certain priority level.


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityPriorityFreeze](#model-linkType)</a> |

###### Response
[SingularityPriorityFreezeParent](#model-SingularityPriorityFreezeParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 200    | The priority freeze request was accepted. | - |
| 400    | There was a validation error with the priority freeze request. | - |


- - -
#### **GET** `/api/priority/freeze`

Get information about the active priority freeze.


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityPriorityFreezeParent](#model-SingularityPriorityFreezeParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 200    | The active priority freeze. | - |
| 404    | There was no active priority freeze. | - |


- - -
### <a name="endpoint-/api/logs"></a> /api/logs
#### Overview
Manages Singularity task logs stored in S3.

#### **GET** `/api/logs/task/{taskId}`

Retrieve the list of logs stored in S3 for a specific task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true | The task ID to search for | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| start | false | Start timestamp (millis, 13 digit) | long |
| end | false | End timestamp (mills, 13 digit) | long |
| excludeMetadata | false | Exclude custom object metadata | boolean |
| list | false | Do not generate download/get urls, only list the files and metadata | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityS3LogMetadata]](#model-SingularityS3LogMetadata)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/logs/search`

Retrieve a paginated list of logs stored in S3


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | true |  | [SingularityS3SearchRequest](#model-linkType)</a> |

###### Response
[SingularityS3SearchResult](#model-SingularityS3SearchResult)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/logs/request/{requestId}/deploy/{deployId}`

Retrieve the list of logs stored in S3 for a specific deploy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to search for | string |
| deployId | true | The deploy ID to search for | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| start | false | Start timestamp (millis, 13 digit) | long |
| end | false | End timestamp (mills, 13 digit) | long |
| excludeMetadata | false | Exclude custom object metadata | boolean |
| list | false | Do not generate download/get urls, only list the files and metadata | boolean |
| maxPerPage | false | Max number of results to return per bucket searched | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityS3LogMetadata]](#model-SingularityS3LogMetadata)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/logs/request/{requestId}`

Retrieve the list of logs stored in S3 for a specific request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to search for | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| start | false | Start timestamp (millis, 13 digit) | long |
| end | false | End timestamp (mills, 13 digit) | long |
| excludeMetadata | false | Exclude custom object metadata | boolean |
| list | false | Do not generate download/get urls, only list the files and metadata | boolean |
| maxPerPage | false | Max number of results to return per bucket searched | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityS3LogMetadata]](#model-SingularityS3LogMetadata)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="endpoint-/api/track"></a> /api/track
#### Overview
Find a task by taskId or runId

#### **GET** `/api/track/task/{taskId}`

Get the current state of a task by taskId whether it is active, or inactive


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityTaskState](#model-SingularityTaskState)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | Task with this id does not exist | - |


- - -
#### **GET** `/api/track/run/{requestId}/{runId}`

Get the current state of a task by taskId whether it is pending, active, or inactive


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true |  | string |
| runId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityTaskState](#model-SingularityTaskState)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | Task with this runId does not exist | - |


- - -
### <a name="endpoint-/api/test"></a> /api/test
#### Overview
Misc testing endpoints.

#### **POST** `/api/test/stop`

Stop the Mesos scheduler driver.


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/start`

Start the Mesos scheduler driver.


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/scheduler/statusUpdate/{taskId}/{taskState}`

Force an update for a specific task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |
| taskState | true |  | string |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/reconcile`

Start task reconciliation


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/purge-history`

Run history purge


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/notleader`

Make this instanceo of Singularity believe it&#39;s lost leadership.


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/leader`

Make this instance of Singularity believe it&#39;s elected leader.


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/exception`

Trigger an exception.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| message | false |  | string |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/abort`

Abort the Mesos scheduler driver.


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="endpoint-/api/history"></a> /api/history
#### Overview
Manages historical data for tasks, requests, and deploys.

#### **GET** `/api/history/tasks/withmetadata`

Retrieve the history sorted by startedAt for all inactive tasks.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | false | Optional Request ID to match | string |
| deployId | false | Optional deploy ID to match | string |
| runId | false | Optional runId to match | string |
| host | false | Optional host to match | string |
| lastTaskStatus | false | Optional last task status to match | string |
| startedBefore | false | Optionally match only tasks started before | long |
| startedAfter | false | Optionally match only tasks started after | long |
| updatedBefore | false | Optionally match tasks last updated before | long |
| updatedAfter | false | Optionally match tasks last updated after | long |
| orderDirection | false | Sort direction | string |
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[SingularityTaskIdHistory]](#model-UNKNOWN[SingularityTaskIdHistory])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/tasks`

Retrieve the history sorted by startedAt for all inactive tasks.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | false | Optional Request ID to match | string |
| deployId | false | Optional deploy ID to match | string |
| runId | false | Optional runId to match | string |
| host | false | Optional host to match | string |
| lastTaskStatus | false | Optional last task status to match | string |
| startedBefore | false | Optionally match only tasks started before | long |
| startedAfter | false | Optionally match only tasks started after | long |
| updatedBefore | false | Optionally match tasks last updated before | long |
| updatedAfter | false | Optionally match tasks last updated after | long |
| orderDirection | false | Sort direction | string |
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityTaskIdHistory]](#model-SingularityTaskIdHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/task/{taskId}`

Retrieve the history for a specific task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true | Task ID to look up | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityTaskHistory](#model-SingularityTaskHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/requests/search`

Search for requests.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestIdLike | false | Request ID prefix to search for | string |
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[string]](#model-UNKNOWN[string])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/tasks/withmetadata`

Retrieve the history count for all inactive tasks of a specific request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to match | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| deployId | false | Optional deploy ID to match | string |
| runId | false | Optional runId to match | string |
| host | false | Optional host to match | string |
| lastTaskStatus | false | Optional last task status to match | string |
| startedBefore | false | Optionally match only tasks started before | long |
| startedAfter | false | Optionally match only tasks started after | long |
| updatedBefore | false | Optionally match tasks last updated before | long |
| updatedAfter | false | Optionally match tasks last updated after | long |
| orderDirection | false | Sort direction | string |
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[SingularityTaskIdHistory]](#model-UNKNOWN[SingularityTaskIdHistory])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/tasks/active`

Retrieve the history for all active tasks of a specific request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to look up | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityTaskIdHistory]](#model-SingularityTaskIdHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/tasks`

Retrieve the history sorted by startedAt for all inactive tasks of a specific request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to match | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| deployId | false | Optional deploy ID to match | string |
| runId | false | Optional runId to match | string |
| host | false | Optional host to match | string |
| lastTaskStatus | false | Optional last task status to match | string |
| startedBefore | false | Optionally match only tasks started before | long |
| startedAfter | false | Optionally match only tasks started after | long |
| updatedBefore | false | Optionally match tasks last updated before | long |
| updatedAfter | false | Optionally match tasks last updated after | long |
| orderDirection | false | Sort direction | string |
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityTaskIdHistory]](#model-SingularityTaskIdHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/run/{runId}`

Retrieve the history for a task by runId


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to look up | string |
| runId | true | runId to look up | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityTaskIdHistory](#model-SingularityTaskIdHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/requests/withmetadata`

Get request history for a single request


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to look up | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[SingularityRequestHistory]](#model-UNKNOWN[SingularityRequestHistory])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/requests`

Get request history for a single request


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to look up | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityRequestHistory]](#model-SingularityRequestHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploys/withmetadata`

Get deploy history with metadata for a single request


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to look up | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[SingularityDeployHistory]](#model-UNKNOWN[SingularityDeployHistory])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploys`

Get deploy history for a single request


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to look up | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityDeployHistory]](#model-SingularityDeployHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploy/{deployId}/tasks/inactive/withmetadata`

Retrieve the task history for a specific deploy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID for deploy | string |
| deployId | true | Deploy ID | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[SingularityTaskIdHistory]](#model-UNKNOWN[SingularityTaskIdHistory])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploy/{deployId}/tasks/inactive`

Retrieve the task history for a specific deploy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID for deploy | string |
| deployId | true | Deploy ID | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityTaskIdHistory]](#model-SingularityTaskIdHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploy/{deployId}/tasks/active`

Retrieve the task history for a specific deploy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID for deploy | string |
| deployId | true | Deploy ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityTaskIdHistory]](#model-SingularityTaskIdHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploy/{deployId}`

Retrieve the history for a specific deploy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID for deploy | string |
| deployId | true | Deploy ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityDeployHistory](#model-SingularityDeployHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/command-line-args`

Get a list of recently used command line args for an on-demand or scheduled request


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to look up | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| count | false | Max number of recent args to return | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[Set](#model-Set)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="endpoint-/api/state"></a> /api/state
#### Overview
Provides information about the current state of Singularity.

#### **GET** `/api/state/task-reconciliation`

Retrieve information about the most recent task reconciliation


###### Parameters
- No parameters

###### Response
[SingularityTaskReconciliationStatistics](#model-SingularityTaskReconciliationStatistics)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/state/requests/under-provisioned`

Retrieve the list of under-provisioned request IDs.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| skipCache | false |  | boolean |

###### Response
List[string]


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/state/requests/over-provisioned`

Retrieve the list of over-provisioned request IDs.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| skipCache | false |  | boolean |

###### Response
List[string]


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/state`

Retrieve information about the current state of Singularity.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| skipCache | false |  | boolean |
| includeRequestIds | false |  | boolean |

###### Response
[SingularityState](#model-SingularityState)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="endpoint-/api/tasks"></a> /api/tasks
#### Overview
Manages Singularity tasks.

#### **GET** `/api/tasks/task/{taskId}/statistics`

Retrieve statistics about a specific active task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[MesosTaskStatisticsObject](#model-MesosTaskStatisticsObject)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/tasks/task/{taskId}/metadata`

Post metadata about a task that will be persisted along with it and displayed in the UI


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityTaskMetadataRequest](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Invalid metadata object or doesn&#39;t match allowed types | - |
| 404    | Task doesn&#39;t exist | - |
| 409    | Metadata with this type/timestamp already existed | - |


- - -
#### **GET** `/api/tasks/task/{taskId}/command/{commandName}/{commandTimestamp}`

Retrieve a list of shell commands updates for a particular shell command on a task


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |
| commandName | true |  | string |
| commandTimestamp | true |  | long |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityTaskShellCommandUpdate]](#model-SingularityTaskShellCommandUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/tasks/task/{taskId}/command`

Run a configured shell command against the given task


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityShellCommand](#model-linkType)</a> |

###### Response
[SingularityTaskShellCommandRequest](#model-SingularityTaskShellCommandRequest)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Given shell command option doesn&#39;t exist | - |
| 403    | Given shell command doesn&#39;t exist | - |


- - -
#### **GET** `/api/tasks/task/{taskId}/command`

Retrieve a list of shell commands that have run for a task


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityTaskShellCommandHistory]](#model-SingularityTaskShellCommandHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/task/{taskId}/cleanup`

Get the cleanup object for the task, if it exists


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityTaskCleanup](#model-SingularityTaskCleanup)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/task/{taskId}`

Retrieve information about a specific active task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityTask](#model-SingularityTask)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/tasks/task/{taskId}`

Attempt to kill task, optionally overriding an existing cleanup request (that may be waiting for replacement tasks to become healthy)


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityTaskCleanup](#model-SingularityTaskCleanup)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 409    | Task already has a cleanup request (can be overridden with override=true) | - |


- - -
#### **GET** `/api/tasks/scheduled/task/{pendingTaskId}`

Retrieve information about a pending task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| pendingTaskId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityTaskRequest](#model-SingularityTaskRequest)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/scheduled/request/{requestId}`

Retrieve list of scheduled tasks for a specific request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true |  | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityTaskRequest]](#model-SingularityTaskRequest)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/scheduled/ids`

Retrieve list of scheduled task IDs.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[SingularityPendingTaskId]](#model-UNKNOWN[SingularityPendingTaskId])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/scheduled`

Retrieve list of scheduled tasks.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityTaskRequest]](#model-SingularityTaskRequest)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/lbcleanup`

Retrieve the list of tasks being cleaned from load balancers.


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[SingularityTaskId]](#model-UNKNOWN[SingularityTaskId])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/killed`

Retrieve the list of killed tasks.


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[SingularityKilledTaskIdRecord]](#model-UNKNOWN[SingularityKilledTaskIdRecord])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/ids/request/{requestId}`

Retrieve a list of task ids separated by status


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityTaskIdsByStatus](#model-SingularityTaskIdsByStatus)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/cleaning`

Retrieve the list of cleaning tasks.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[SingularityTaskCleanup]](#model-UNKNOWN[SingularityTaskCleanup])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/active/slave/{slaveId}`

Retrieve list of active tasks on a specific slave.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| slaveId | true |  | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[SingularityTask]](#model-UNKNOWN[SingularityTask])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/active`

Retrieve the list of active tasks.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[SingularityTask]](#model-UNKNOWN[SingularityTask])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="endpoint-/api/sandbox"></a> /api/sandbox
#### Overview
Provides a proxy to Mesos sandboxes.

#### **GET** `/api/sandbox/{taskId}/read`

Retrieve part of the contents of a file in a specific task&#39;s sandbox.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true | The task ID of the sandbox to read from | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| path | false | The path to the file to be read | string |
| grep | false | Optional string to grep for | string |
| offset | false | Byte offset to start reading from | long |
| length | false | Maximum number of bytes to read | long |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[MesosFileChunkObject](#model-MesosFileChunkObject)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/sandbox/{taskId}/browse`

Retrieve information about a specific task&#39;s sandbox.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true | The task ID to browse | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| path | false | The path to browse from | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularitySandbox](#model-SingularitySandbox)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="endpoint-/api/slaves"></a> /api/slaves
#### Overview
Manages Singularity slaves.

#### **POST** `/api/slaves/slave/{slaveId}/freeze`

Freeze tasks on a specific slave


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| slaveId | true | Slave ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityMachineChangeRequest](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/slaves/slave/{slaveId}/expiring`

Delete any expiring machine state changes for this slave


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| slaveId | true | Active slaveId | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/slaves/slave/{slaveId}/details`

Get information about a particular slave


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| slaveId | true | Slave ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularitySlave](#model-SingularitySlave)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/slaves/slave/{slaveId}/decommission`

Begin decommissioning a specific active slave


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| slaveId | true | Active slaveId | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityMachineChangeRequest](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/slaves/slave/{slaveId}/activate`

Activate a decomissioning slave, canceling decomission without erasing history


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| slaveId | true | Active slaveId | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityMachineChangeRequest](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/slaves/slave/{slaveId}`

Retrieve the history of a given slave


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| slaveId | true | Slave ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityMachineStateHistoryUpdate]](#model-SingularityMachineStateHistoryUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/slaves/slave/{slaveId}`

Remove a known slave, erasing history. This operation will cancel decomissioning of the slave


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| slaveId | true | Active SlaveId | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/slaves/expiring`

Get all expiring state changes for all slaves


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityExpiringMachineState]](#model-SingularityExpiringMachineState)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/slaves/`

Retrieve the list of all known slaves, optionally filtering by a particular state


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| state | false | Optionally specify a particular state to filter slaves by | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularitySlave]](#model-SingularitySlave)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="endpoint-/api/disasters"></a> /api/disasters
#### Overview
Manages Singularity Deploys for existing requests

#### **POST** `/api/disasters/task-credits`

Add task credits, enables task credit system if not already enabled


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| credits | false |  | int |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/disasters/task-credits`

Disable task credit system


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/disasters/task-credits`

Get task credit data


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityTaskCredits](#model-SingularityTaskCredits)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/disasters/stats`

Get current data related to disaster detection


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityDisastersData](#model-SingularityDisastersData)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/disasters/enable`

Allow the automated poller to disable actions when a disaster is detected


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/disasters/disabled-actions/{action}`

Disable a specific action


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| action | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityDisabledActionRequest](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/disasters/disabled-actions/{action}`

Re-enable a specific action if it has been disabled


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| action | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/disasters/disabled-actions`

Get a list of actions that are currently disable


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityDisabledAction]](#model-SingularityDisabledAction)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/disasters/disable`

Do not allow the automated poller to disable actions when a disaster is detected


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/disasters/active/{type}`

Remove an active disaster (make it inactive)


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| type | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/disasters/active/{type}`

Create a new active disaster


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| type | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/disasters/active`

Get a list of current active disasters


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
List[string]


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="endpoint-/api/requests"></a> /api/requests
#### Overview
Manages Singularity Requests, the parent object for any deployed task

#### **POST** `/api/requests/request/{requestId}/unpause`

Unpause a Singularity Request, scheduling new tasks immediately


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to unpause | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUnpauseRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 409    | Request is not paused | - |


- - -
#### **DELETE** `/api/requests/request/{requestId}/skipHealthchecks`

Delete/cancel the expiring skipHealthchecks. This makes the skipHealthchecks request permanent.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request or expiring skipHealthchecks request for that ID | - |


- - -
#### **PUT** `/api/requests/request/{requestId}/skipHealthchecks`

Update the skipHealthchecks field for the request, possibly temporarily


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID to scale | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | SkipHealtchecks options | [SingularitySkipHealthchecksRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request with that ID | - |


- - -
#### **PUT** `/api/requests/request/{requestId}/skip-healthchecks`

Update the skipHealthchecks field for the request, possibly temporarily


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID to scale | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | SkipHealtchecks options | [SingularitySkipHealthchecksRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request with that ID | - |


- - -
#### **DELETE** `/api/requests/request/{requestId}/skip-healthchecks`

Delete/cancel the expiring skipHealthchecks. This makes the skipHealthchecks request permanent.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request or expiring skipHealthchecks request for that ID | - |


- - -
#### **PUT** `/api/requests/request/{requestId}/scale`

Scale the number of instances up or down for a specific Request


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID to scale | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Object to hold number of instances to request | [SingularityScaleRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request with that ID | - |


- - -
#### **DELETE** `/api/requests/request/{requestId}/scale`

Delete/cancel the expiring scale. This makes the scale request permanent.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request or expiring scale request for that ID | - |


- - -
#### **GET** `/api/requests/request/{requestId}/run/{runId}`

Retrieve an active task by runId


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true |  | string |
| runId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityTaskId](#model-SingularityTaskId)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/requests/request/{requestId}/run`

Schedule a one-off or scheduled Singularity request for immediate or delayed execution.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to run | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityRunNowRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Singularity Request is not scheduled or one-off | - |


- - -
#### **POST** `/api/requests/request/{requestId}/pause`

Pause a Singularity request, future tasks will not run until it is manually unpaused. API can optionally choose to kill existing tasks


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to pause | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Pause Request Options | [SingularityPauseRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 409    | Request is already paused or being cleaned | - |


- - -
#### **DELETE** `/api/requests/request/{requestId}/pause`

Delete/cancel the expiring pause. This makes the pause request permanent.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request or expiring pause request for that ID | - |


- - -
#### **POST** `/api/requests/request/{requestId}/exit-cooldown`

Immediately exits cooldown, scheduling new tasks immediately


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityExitCooldownRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 409    | Request is not in cooldown | - |


- - -
#### **POST** `/api/requests/request/{requestId}/bounce`

Bounce a specific Singularity request. A bounce launches replacement task(s), and then kills the original task(s) if the replacement(s) are healthy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to bounce | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Bounce request options | [SingularityBounceRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/requests/request/{requestId}/bounce`

Delete/cancel the expiring bounce. This makes the bounce request permanent.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request or expiring bounce request for that ID | - |


- - -
#### **GET** `/api/requests/request/{requestId}`

Retrieve a specific Request by ID


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request with that ID | - |


- - -
#### **DELETE** `/api/requests/request/{requestId}`

Delete a specific Request by ID and return the deleted Request


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to delete. | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Delete options | [SingularityDeleteRequestRequest](#model-linkType)</a> |

###### Response
[SingularityRequest](#model-SingularityRequest)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request with that ID | - |


- - -
#### **GET** `/api/requests/queued/pending`

Retrieve the list of pending requests


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityPendingRequest]](#model-SingularityPendingRequest)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/queued/cleanup`

Retrieve the list of requests being cleaned up


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityRequestCleanup]](#model-SingularityRequestCleanup)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/paused`

Retrieve the list of paused requests


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityRequestParent]](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/lbcleanup`

Retrieve the list of tasks being cleaned from load balancers.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[UNKNOWN[string]](#model-UNKNOWN[string])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/finished`

Retreive the list of finished requests (Scheduled requests which have exhausted their schedules)


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityRequestParent]](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/cooldown`

Retrieve the list of requests in system cooldown


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityRequestParent]](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/active`

Retrieve the list of active requests


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityRequestParent]](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests`

Retrieve the list of all requests


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityRequestParent]](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/requests`

Create or update a Singularity Request


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | The Singularity request to create or update | [SingularityRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Request object is invalid | - |
| 409    | Request object is being cleaned. Try again shortly | - |


- - -
### <a name="endpoint-/api/groups"></a> /api/groups
#### Overview
Manages Singularity Request Groups, which are collections of one or more Singularity Requests

#### **GET** `/api/groups/group/{requestGroupId}`

Get a specific Singularity request group by ID


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestGroupId | true |  | string |

###### Response
[SingularityRequestGroup](#model-SingularityRequestGroup)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/groups/group/{requestGroupId}`

Delete a specific Singularity request group by ID


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestGroupId | true |  | string |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/groups`

Get a list of Singularity request groups


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| useWebCache | false |  | boolean |

###### Response
[List[SingularityRequestGroup]](#model-SingularityRequestGroup)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/groups`

Create a Singularity request group


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityRequestGroup](#model-linkType)</a> |

###### Response
[SingularityRequestGroup](#model-SingularityRequestGroup)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="endpoint-/api/deploys"></a> /api/deploys
#### Overview
Manages Singularity Deploys for existing requests

#### **POST** `/api/deploys/update`

Update the target active instance count for a pending deploy


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | true |  | [SingularityUpdatePendingDeployRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Deploy is not in the pending state pending or is not not present | - |


- - -
#### **GET** `/api/deploys/pending`

Retrieve the list of current pending deploys


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[List[SingularityPendingDeploy]](#model-SingularityPendingDeploy)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/deploys/deploy/{deployId}/request/{requestId}`

Cancel a pending deployment (best effort - the deploy may still succeed or fail)


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Singularity Request Id from which the deployment is removed. | string |
| deployId | true | The Singularity Deploy Id that should be removed. | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUser](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Deploy is not in the pending state pending or is not not present | - |


- - -
#### **POST** `/api/deploys`

Start a new deployment for a Request


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | true |  | [SingularityDeployRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Deploy object is invalid | - |
| 409    | A current deploy is in progress. It may be canceled by calling DELETE | - |


- - -

## Data Types

## <a name="model-EmbeddedArtifact"></a> EmbeddedArtifact

| name | type | required | description |
|------|------|----------|-------------|
| targetFolderRelativeToTask | string | optional |  |
| md5sum | string | optional |  |
| filename | string | optional |  |
| name | string | optional |  |
| content | [Array[byte]](#model-byte) | optional |  |


## <a name="model-ExecutorData"></a> ExecutorData

| name | type | required | description |
|------|------|----------|-------------|
| skipLogrotateAndCompress | boolean | optional | If true, do not run logrotate or compress old log files |
| loggingExtraFields | [Map[string,string]](#model-Map[string,string]) | optional |  |
| embeddedArtifacts | [Array[EmbeddedArtifact]](#model-EmbeddedArtifact) | optional | A list of the full content of any embedded artifacts |
| s3Artifacts | [Array[S3Artifact]](#model-S3Artifact) | optional | List of s3 artifacts for the executor to download |
| successfulExitCodes | Array[int] | optional | Allowable exit codes for the task to be considered FINISHED instead of FAILED |
| runningSentinel | string | optional |  |
| logrotateFrequency | [SingularityExecutorLogrotateFrequency](#model-SingularityExecutorLogrotateFrequency) | optional | Run logrotate this often. Can be HOURLY, DAILY, WEEKLY, MONTHLY |
| maxOpenFiles | int | optional | Maximum number of open files the task process is allowed |
| externalArtifacts | [Array[ExternalArtifact]](#model-ExternalArtifact) | optional | A list of external artifacts for the executor to download |
| user | string | optional | Run the task process as this user |
| preserveTaskSandboxAfterFinish | boolean | optional | If true, do not delete files in the task sandbox after the task process has terminated |
| extraCmdLineArgs | Array[string] | optional | Extra arguments in addition to any provided in the cmd field |
| loggingTag | string | optional |  |
| sigKillProcessesAfterMillis | long | optional | Send a sigkill to a process if it has not shut down this many millis after being sent a term signal |
| maxTaskThreads | int | optional | Maximum number of threads a task is allowed to use |
| s3ArtifactSignatures | [Array[S3ArtifactSignature]](#model-S3ArtifactSignature) | optional | A list of signatures use to verify downloaded s3artifacts |
| cmd | string | required | Command for the custom executor to run |


## <a name="model-ExternalArtifact"></a> ExternalArtifact

| name | type | required | description |
|------|------|----------|-------------|
| targetFolderRelativeToTask | string | optional |  |
| md5sum | string | optional |  |
| url | string | optional |  |
| filename | string | optional |  |
| filesize | long | optional |  |
| name | string | optional |  |
| isArtifactList | boolean | optional |  |


## <a name="model-HealthcheckOptions"></a> HealthcheckOptions

| name | type | required | description |
|------|------|----------|-------------|
| startupDelaySeconds | int | optional | Wait this long before issuing the first healthcheck |
| responseTimeoutSeconds | int | optional | Single healthcheck HTTP timeout in seconds. |
| intervalSeconds | int | optional | Time to wait after a valid but failed healthcheck response to try again in seconds. |
| uri | string | required | Healthcheck uri to hit |
| failureStatusCodes | Array[int] | optional | Fail the healthcheck with no further retries if one of these status codes is returned |
| maxRetries | int | optional | Maximum number of times to retry an individual healthcheck before failing the deploy. |
| startupTimeoutSeconds | int | optional | Consider the task unhealthy/failed if the app has not started responding to healthchecks in this amount of time |
| portNumber | long | optional | Perform healthcheck on this port (portIndex cannot also be used when using this setting) |
| startupIntervalSeconds | int | optional | Time to wait after a failed healthcheck to try again in seconds. |
| protocol | [HealthcheckProtocol](#model-HealthcheckProtocol) | optional | Healthcheck protocol - HTTP or HTTPS |
| portIndex | int | optional | Perform healthcheck on this dynamically allocated port (e.g. 0 for first port), defaults to first port |


## <a name="model-LoadBalancerRequestId"></a> LoadBalancerRequestId

| name | type | required | description |
|------|------|----------|-------------|
| requestType | [LoadBalancerRequestType](#model-LoadBalancerRequestType) | optional |  Allowable values: ADD, REMOVE, DEPLOY, DELETE |
| attemptNumber | int | optional |  |
| id | string | optional |  |


## <a name="model-MesosFileChunkObject"></a> MesosFileChunkObject

| name | type | required | description |
|------|------|----------|-------------|
| nextOffset | long | optional |  |
| data | string | optional |  |
| offset | long | optional |  |


## <a name="model-MesosResourcesObject"></a> MesosResourcesObject

| name | type | required | description |
|------|------|----------|-------------|
| properties | [Map[string,Object]](#model-Map[string,Object]) | optional |  |


## <a name="model-MesosTaskStatisticsObject"></a> MesosTaskStatisticsObject

| name | type | required | description |
|------|------|----------|-------------|
| memFileBytes | long | optional |  |
| cpusThrottledTimeSecs | double | optional |  |
| memLimitBytes | long | optional |  |
| cpusSystemTimeSecs | double | optional |  |
| memRssBytes | long | optional |  |
| memAnonBytes | long | optional |  |
| memMappedFileBytes | long | optional |  |
| cpusLimit | int | optional |  |
| cpusNrPeriods | long | optional |  |
| timestamp | double | optional |  |
| cpusUserTimeSecs | double | optional |  |
| cpusNrThrottled | long | optional |  |
| memTotalBytes | long | optional |  |


## <a name="model-Resources"></a> Resources

| name | type | required | description |
|------|------|----------|-------------|
| numPorts | int | optional |  |
| memoryMb | double | optional |  |
| diskMb | double | optional |  |
| cpus | double | optional |  |


## <a name="model-S3Artifact"></a> S3Artifact

| name | type | required | description |
|------|------|----------|-------------|
| targetFolderRelativeToTask | string | optional |  |
| s3Bucket | string | optional |  |
| md5sum | string | optional |  |
| filename | string | optional |  |
| filesize | long | optional |  |
| s3ObjectKey | string | optional |  |
| name | string | optional |  |
| isArtifactList | boolean | optional |  |


## <a name="model-S3ArtifactSignature"></a> S3ArtifactSignature

| name | type | required | description |
|------|------|----------|-------------|
| targetFolderRelativeToTask | string | optional |  |
| s3Bucket | string | optional |  |
| md5sum | string | optional |  |
| filename | string | optional |  |
| filesize | long | optional |  |
| s3ObjectKey | string | optional |  |
| name | string | optional |  |
| artifactFilename | string | optional |  |
| isArtifactList | boolean | optional |  |


## <a name="model-Set"></a> Set

| name | type | required | description |
|------|------|----------|-------------|
| empty | boolean | optional |  |


## <a name="model-SingularityBounceRequest"></a> SingularityBounceRequest

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | Instruct replacement tasks for this bounce only to skip healthchecks |
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |
| runShellCommandBeforeKill | [SingularityShellCommand](#model-SingularityShellCommand) | optional | Attempt to run this shell command on each task before it is shut down |
| incremental | boolean | optional | If present and set to true, old tasks will be killed as soon as replacement tasks are available, instead of waiting for all replacement tasks to be healthy |


## <a name="model-SingularityContainerInfo"></a> SingularityContainerInfo

| name | type | required | description |
|------|------|----------|-------------|
| type | [SingularityContainerType](#model-SingularityContainerType) | required | Container type, can be MESOS or DOCKER. Default is MESOS Allowable values: MESOS, DOCKER |
| volumes | [Array[SingularityVolume]](#model-SingularityVolume) | optional | List of volumes to mount. Applicable only to DOCKER container type |
| docker | [SingularityDockerInfo](#model-SingularityDockerInfo) | optional | Information specific to docker runtime settings |


## <a name="model-SingularityDeleteRequestRequest"></a> SingularityDeleteRequestRequest

| name | type | required | description |
|------|------|----------|-------------|
| deleteFromLoadBalancer | boolean | optional | Should the service associated with the request be removed from the load balancer |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularityDeploy"></a> SingularityDeploy

| name | type | required | description |
|------|------|----------|-------------|
| customExecutorId | string | optional | Custom Mesos executor id. |
| resources | [com.hubspot.mesos.Resources](#model-com.hubspot.mesos.Resources) | optional | Resources required for this deploy. |
| uris | [Array[SingularityMesosArtifact]](#model-SingularityMesosArtifact) | optional | List of URIs to download before executing the deploy command. |
| containerInfo | [SingularityContainerInfo](#model-SingularityContainerInfo) | optional | Container information for deployment into a container. |
| loadBalancerDomains | [Set](#model-Set) | optional | List of domains to host this service on, for use with the load balancer api |
| healthcheck | [HealthcheckOptions](#model-HealthcheckOptions) | optional | HTTP Healthcheck settings |
| arguments | Array[string] | optional | Command arguments. |
| taskEnv | [Map[int,Map[string,string]]](#model-Map[int,Map[string,string]]) | optional | Map of environment variable overrides for specific task instances. |
| autoAdvanceDeploySteps | boolean | optional | automatically advance to the next target instance count after `deployStepWaitTimeMs` seconds |
| serviceBasePath | string | optional | The base path for the API exposed by the deploy. Used in conjunction with the Load balancer API. |
| customExecutorSource | string | optional | Custom Mesos executor source. |
| metadata | [Map[string,string]](#model-Map[string,string]) | optional | Map of metadata key/value pairs associated with the deployment. |
| healthcheckMaxRetries | int | optional | Maximum number of times to retry an individual healthcheck before failing the deploy. |
| healthcheckTimeoutSeconds | long | optional | Single healthcheck HTTP timeout in seconds. |
| healthcheckProtocol | [com.hubspot.singularity.HealthcheckProtocol](#model-com.hubspot.singularity.HealthcheckProtocol) | optional | Healthcheck protocol - HTTP or HTTPS |
| taskLabels | [Map[int,Map[string,string]]](#model-Map[int,Map[string,string]]) | optional | (Deprecated) Labels for specific tasks associated with this deploy, indexed by instance number |
| healthcheckPortIndex | int | optional | Perform healthcheck on this dynamically allocated port (e.g. 0 for first port), defaults to first port |
| healthcheckMaxTotalTimeoutSeconds | long | optional | Maximum amount of time to wait before failing a deploy for healthchecks to pass. |
| loadBalancerServiceIdOverride | string | optional | Name of load balancer Service ID to use instead of the Request ID |
| mesosTaskLabels | [Map[int,List[SingularityMesosTaskLabel]]](#model-Map[int,List[SingularityMesosTaskLabel]]) | optional | Labels for specific tasks associated with this deploy, indexed by instance number |
| labels | [Map[string,string]](#model-Map[string,string]) | optional | Labels for all tasks associated with this deploy |
| healthcheckUri | string | optional | Deployment Healthcheck URI, if specified will be called after TASK_RUNNING. |
| user | string | optional | Run tasks as this user |
| requestId | string | required | Singularity Request Id which is associated with this deploy. |
| loadBalancerGroups | [Set](#model-Set) | optional | List of load balancer groups associated with this deployment. |
| deployStepWaitTimeMs | int | optional | wait this long between deploy steps |
| skipHealthchecksOnDeploy | boolean | optional | Allows skipping of health checks when deploying. |
| mesosLabels | [Array[SingularityMesosTaskLabel]](#model-SingularityMesosTaskLabel) | optional | Labels for all tasks associated with this deploy |
| healthcheckIntervalSeconds | long | optional | Time to wait after a failed healthcheck to try again in seconds. |
| command | string | optional | Command to execute for this deployment. |
| executorData | [ExecutorData](#model-ExecutorData) | optional | Executor specific information |
| loadBalancerAdditionalRoutes | Array[string] | optional | Additional routes besides serviceBasePath used by this service |
| shell | boolean | optional | Override the shell property on the mesos task |
| timestamp | long | optional | Deploy timestamp. |
| deployInstanceCountPerStep | int | optional | deploy this many instances at a time |
| considerHealthyAfterRunningForSeconds | long | optional | Number of seconds that a service must be healthy to consider the deployment to be successful. |
| loadBalancerOptions | [Map[string,Object]](#model-Map[string,Object]) | optional | Map (Key/Value) of options for the load balancer. |
| maxTaskRetries | int | optional | allowed at most this many failed tasks to be retried before failing the deploy |
| runImmediately | [SingularityRunNowRequest](#model-SingularityRunNowRequest) | optional | Settings used to run this deploy immediately |
| loadBalancerPortIndex | int | optional | Send this port to the load balancer api (e.g. 0 for first port), defaults to first port |
| loadBalancerTemplate | string | optional | Name of load balancer template to use if not using the default template |
| customExecutorCmd | string | optional | Custom Mesos executor |
| env | [Map[string,string]](#model-Map[string,string]) | optional | Map of environment variable definitions. |
| loadBalancerUpstreamGroup | string | optional | Group name to tag all upstreams with in load balancer |
| customExecutorResources | [com.hubspot.mesos.Resources](#model-com.hubspot.mesos.Resources) | optional | Resources to allocate for custom mesos executor |
| version | string | optional | Deploy version |
| id | string | required | Singularity deploy id. |
| deployHealthTimeoutSeconds | long | optional | Number of seconds that Singularity waits for this service to become healthy (for it to download artifacts, start running, and optionally pass healthchecks.) |


## <a name="model-SingularityDeployFailure"></a> SingularityDeployFailure

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| message | string | optional |  |
| reason | [SingularityDeployFailureReason](#model-SingularityDeployFailureReason) | optional |  Allowable values: TASK_FAILED_ON_STARTUP, TASK_FAILED_HEALTH_CHECKS, TASK_COULD_NOT_BE_SCHEDULED, TASK_NEVER_ENTERED_RUNNING, TASK_EXPECTED_RUNNING_FINISHED, DEPLOY_CANCELLED, DEPLOY_OVERDUE, FAILED_TO_SAVE_DEPLOY_STATE, LOAD_BALANCER_UPDATE_FAILED, PENDING_DEPLOY_REMOVED |


## <a name="model-SingularityDeployHistory"></a> SingularityDeployHistory

| name | type | required | description |
|------|------|----------|-------------|
| deploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| deployStatistics | [SingularityDeployStatistics](#model-SingularityDeployStatistics) | optional |  |
| deployResult | [SingularityDeployResult](#model-SingularityDeployResult) | optional |  |
| deployMarker | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |


## <a name="model-SingularityDeployMarker"></a> SingularityDeployMarker

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| requestId | string | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |
| deployId | string | optional |  |


## <a name="model-SingularityDeployProgress"></a> SingularityDeployProgress

| name | type | required | description |
|------|------|----------|-------------|
| autoAdvanceDeploySteps | boolean | optional |  |
| stepComplete | boolean | optional |  |
| deployStepWaitTimeMs | long | optional |  |
| timestamp | long | optional |  |
| deployInstanceCountPerStep | int | optional |  |
| failedDeployTasks | [Set](#model-Set) | optional |  |
| currentActiveInstances | int | optional |  |
| targetActiveInstances | int | optional |  |


## <a name="model-SingularityDeployRequest"></a> SingularityDeployRequest

| name | type | required | description |
|------|------|----------|-------------|
| unpauseOnSuccessfulDeploy | boolean | optional | If deploy is successful, also unpause the request |
| deploy | [SingularityDeploy](#model-SingularityDeploy) | required | The Singularity deploy object, containing all the required details about the Deploy |
| updatedRequest | [SingularityRequest](#model-SingularityRequest) | optional | use this request data for this deploy, and update the request on successful deploy |
| message | string | optional | A message to show users about this deploy (metadata) |


## <a name="model-SingularityDeployResult"></a> SingularityDeployResult

| name | type | required | description |
|------|------|----------|-------------|
| lbUpdate | [SingularityLoadBalancerUpdate](#model-SingularityLoadBalancerUpdate) | optional |  |
| deployState | [DeployState](#model-DeployState) | optional |  Allowable values: SUCCEEDED, FAILED_INTERNAL_STATE, CANCELING, WAITING, OVERDUE, FAILED, CANCELED |
| deployFailures | [Array[SingularityDeployFailure]](#model-SingularityDeployFailure) | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |


## <a name="model-SingularityDeployStatistics"></a> SingularityDeployStatistics

| name | type | required | description |
|------|------|----------|-------------|
| lastTaskState | [ExtendedTaskState](#model-ExtendedTaskState) | optional |  |
| numFailures | int | optional |  |
| numTasks | int | optional |  |
| averageSchedulingDelayMillis | long | optional |  |
| averageRuntimeMillis | long | optional |  |
| lastFinishAt | long | optional |  |
| requestId | string | optional |  |
| deployId | string | optional |  |
| numSequentialRetries | int | optional |  |
| numSuccess | int | optional |  |
| instanceSequentialFailureTimestamps | [com.google.common.collect.ListMultimap&lt;java.lang.Integer, java.lang.Long&gt;](#model-com.google.common.collect.ListMultimap&lt;java.lang.Integer, java.lang.Long&gt;) | optional |  |


## <a name="model-SingularityDeployUpdate"></a> SingularityDeployUpdate

| name | type | required | description |
|------|------|----------|-------------|
| deploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| deployResult | [SingularityDeployResult](#model-SingularityDeployResult) | optional |  |
| eventType | [DeployEventType](#model-DeployEventType) | optional |  Allowable values: STARTING, FINISHED |
| deployMarker | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |


## <a name="model-SingularityDisabledAction"></a> SingularityDisabledAction

| name | type | required | description |
|------|------|----------|-------------|
| expiresAt | long | optional |  |
| type | [SingularityAction](#model-SingularityAction) | optional |  Allowable values: BOUNCE_REQUEST, SCALE_REQUEST, REMOVE_REQUEST, CREATE_REQUEST, UPDATE_REQUEST, VIEW_REQUEST, PAUSE_REQUEST, KILL_TASK, BOUNCE_TASK, RUN_SHELL_COMMAND, ADD_METADATA, DEPLOY, CANCEL_DEPLOY, ADD_WEBHOOK, REMOVE_WEBHOOK, VIEW_WEBHOOKS, TASK_RECONCILIATION, STARTUP_TASK_RECONCILIATION, RUN_HEALTH_CHECKS, ADD_DISASTER, REMOVE_DISASTER, DISABLE_ACTION, ENABLE_ACTION, VIEW_DISASTERS, FREEZE_SLAVE, ACTIVATE_SLAVE, DECOMMISSION_SLAVE, VIEW_SLAVES, FREEZE_RACK, ACTIVATE_RACK, DECOMMISSION_RACK, VIEW_RACKS, SEND_EMAIL, PROCESS_OFFERS, CACHE_OFFERS, EXPENSIVE_API_CALLS, RUN_CLEANUP_POLLER, RUN_DEPLOY_POLLER, RUN_SCHEDULER_POLLER, RUN_EXPIRING_ACTION_POLLER |
| automaticallyClearable | boolean | optional |  |
| user | string | optional |  |
| message | string | optional |  |


## <a name="model-SingularityDisabledActionRequest"></a> SingularityDisabledActionRequest

| name | type | required | description |
|------|------|----------|-------------|
| type | [SingularityAction](#model-SingularityAction) | required | The type of action to disable Allowable values: BOUNCE_REQUEST, SCALE_REQUEST, REMOVE_REQUEST, CREATE_REQUEST, UPDATE_REQUEST, VIEW_REQUEST, PAUSE_REQUEST, KILL_TASK, BOUNCE_TASK, RUN_SHELL_COMMAND, ADD_METADATA, DEPLOY, CANCEL_DEPLOY, ADD_WEBHOOK, REMOVE_WEBHOOK, VIEW_WEBHOOKS, TASK_RECONCILIATION, STARTUP_TASK_RECONCILIATION, RUN_HEALTH_CHECKS, ADD_DISASTER, REMOVE_DISASTER, DISABLE_ACTION, ENABLE_ACTION, VIEW_DISASTERS, FREEZE_SLAVE, ACTIVATE_SLAVE, DECOMMISSION_SLAVE, VIEW_SLAVES, FREEZE_RACK, ACTIVATE_RACK, DECOMMISSION_RACK, VIEW_RACKS, SEND_EMAIL, PROCESS_OFFERS, CACHE_OFFERS, EXPENSIVE_API_CALLS, RUN_CLEANUP_POLLER, RUN_DEPLOY_POLLER, RUN_SCHEDULER_POLLER, RUN_EXPIRING_ACTION_POLLER |
| message | string | optional | An optional message/reason for disabling the action specified |


## <a name="model-SingularityDisaster"></a> SingularityDisaster

| name | type | required | description |
|------|------|----------|-------------|
| type | [SingularityDisasterType](#model-SingularityDisasterType) | optional |  Allowable values: EXCESSIVE_TASK_LAG, LOST_SLAVES, LOST_TASKS, USER_INITIATED |
| active | boolean | optional |  |


## <a name="model-SingularityDisasterDataPoint"></a> SingularityDisasterDataPoint

| name | type | required | description |
|------|------|----------|-------------|
| numLateTasks | int | optional |  |
| numPendingTasks | int | optional |  |
| numActiveTasks | int | optional |  |
| numLostSlaves | int | optional |  |
| numActiveSlaves | int | optional |  |
| timestamp | long | optional |  |
| numLostTasks | int | optional |  |
| avgTaskLagMillis | long | optional |  |


## <a name="model-SingularityDisastersData"></a> SingularityDisastersData

| name | type | required | description |
|------|------|----------|-------------|
| automatedActionsDisabled | boolean | optional |  |
| disasters | [Array[SingularityDisaster]](#model-SingularityDisaster) | optional |  |
| stats | [Array[SingularityDisasterDataPoint]](#model-SingularityDisasterDataPoint) | optional |  |


## <a name="model-SingularityDockerInfo"></a> SingularityDockerInfo

| name | type | required | description |
|------|------|----------|-------------|
| parameters | [Map[string,string]](#model-Map[string,string]) | optional |  |
| forcePullImage | boolean | optional | Always run docker pull even if the image already exists locally |
| dockerParameters | [Array[SingularityDockerParameter]](#model-SingularityDockerParameter) | optional | Other docker run command line options to be set |
| privileged | boolean | required | Controls use of the docker --privleged flag |
| network | [com.hubspot.mesos.SingularityDockerNetworkType](#model-com.hubspot.mesos.SingularityDockerNetworkType) | optional | Docker netowkr type. Value can be BRIDGE, HOST, or NONE |
| portMappings | [Array[SingularityDockerPortMapping]](#model-SingularityDockerPortMapping) | optional | List of port mappings |
| image | string | required | Docker image name |


## <a name="model-SingularityDockerParameter"></a> SingularityDockerParameter

| name | type | required | description |
|------|------|----------|-------------|
| key | string | optional |  |
| value | string | optional |  |


## <a name="model-SingularityDockerPortMapping"></a> SingularityDockerPortMapping

| name | type | required | description |
|------|------|----------|-------------|
| hostPort | int | required | Port number, or index of port from offer on the host |
| containerPort | int | required | Port number, or index of port from offer within the container |
| containerPortType | [SingularityPortMappingType](#model-SingularityPortMappingType) | optional | Container port. Use the port number provided (LITERAL) or the dynamically allocated port at this index (FROM_OFFER) Allowable values: LITERAL, FROM_OFFER |
| protocol | string | optional | Protocol for binding the port. Default is tcp |
| hostPortType | [SingularityPortMappingType](#model-SingularityPortMappingType) | optional | Host port. Use the port number provided (LITERAL) or the dynamically allocated port at this index (FROM_OFFER) Allowable values: LITERAL, FROM_OFFER |


## <a name="model-SingularityExitCooldownRequest"></a> SingularityExitCooldownRequest

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | Instruct new tasks that are scheduled immediately while executing cooldown to skip healthchecks |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularityExpiringBounce"></a> SingularityExpiringBounce

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| requestId | string | optional |  |
| startMillis | long | optional |  |
| deployId | string | optional |  |
| actionId | string | optional |  |
| expiringAPIRequestObject | [T](#model-T) | optional |  |


## <a name="model-SingularityExpiringMachineState"></a> SingularityExpiringMachineState

| name | type | required | description |
|------|------|----------|-------------|
| revertToState | [MachineState](#model-MachineState) | optional |  Allowable values: MISSING_ON_STARTUP, ACTIVE, STARTING_DECOMMISSION, DECOMMISSIONING, DECOMMISSIONED, DEAD, FROZEN |
| user | string | optional |  |
| startMillis | long | optional |  |
| actionId | string | optional |  |
| expiringAPIRequestObject | [T](#model-T) | optional |  |
| machineId | string | optional |  |
| killTasksOnDecommissionTimeout | boolean | optional |  |


## <a name="model-SingularityExpiringPause"></a> SingularityExpiringPause

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| requestId | string | optional |  |
| startMillis | long | optional |  |
| actionId | string | optional |  |
| expiringAPIRequestObject | [T](#model-T) | optional |  |


## <a name="model-SingularityExpiringScale"></a> SingularityExpiringScale

| name | type | required | description |
|------|------|----------|-------------|
| revertToInstances | int | optional |  |
| user | string | optional |  |
| requestId | string | optional |  |
| bounce | boolean | optional |  |
| startMillis | long | optional |  |
| actionId | string | optional |  |
| expiringAPIRequestObject | [T](#model-T) | optional |  |


## <a name="model-SingularityExpiringSkipHealthchecks"></a> SingularityExpiringSkipHealthchecks

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| requestId | string | optional |  |
| startMillis | long | optional |  |
| actionId | string | optional |  |
| expiringAPIRequestObject | [T](#model-T) | optional |  |
| revertToSkipHealthchecks | boolean | optional |  |


## <a name="model-SingularityHostState"></a> SingularityHostState

| name | type | required | description |
|------|------|----------|-------------|
| availableCachedMemory | double | optional |  |
| hostAddress | string | optional |  |
| hostname | string | optional |  |
| mesosConnected | boolean | optional |  |
| driverStatus | string | optional |  |
| master | boolean | optional |  |
| mesosMaster | string | optional |  |
| uptime | long | optional |  |
| availableCachedCpus | double | optional |  |
| offerCacheSize | int | optional |  |
| millisSinceLastOffer | long | optional |  |


## <a name="model-SingularityLoadBalancerUpdate"></a> SingularityLoadBalancerUpdate

| name | type | required | description |
|------|------|----------|-------------|
| loadBalancerState | [BaragonRequestState](#model-BaragonRequestState) | optional |  Allowable values: UNKNOWN, FAILED, WAITING, SUCCESS, CANCELING, CANCELED, INVALID_REQUEST_NOOP |
| loadBalancerRequestId | [LoadBalancerRequestId](#model-LoadBalancerRequestId) | optional |  |
| uri | string | optional |  |
| method | [LoadBalancerMethod](#model-LoadBalancerMethod) | optional |  Allowable values: PRE_ENQUEUE, ENQUEUE, CHECK_STATE, CANCEL, DELETE |
| message | string | optional |  |
| timestamp | long | optional |  |


## <a name="model-SingularityMachineChangeRequest"></a> SingularityMachineChangeRequest

| name | type | required | description |
|------|------|----------|-------------|
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| revertToState | [MachineState](#model-MachineState) | optional | If a durationMillis is specified, return to this state when time has elapsed |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |
| killTasksOnDecommissionTimeout | boolean | optional | If a machine has not successfully decommissioned in durationMillis, kill the remaining tasks on the machine |


## <a name="model-SingularityMachineStateHistoryUpdate"></a> SingularityMachineStateHistoryUpdate

| name | type | required | description |
|------|------|----------|-------------|
| state | [MachineState](#model-MachineState) | optional |  Allowable values: MISSING_ON_STARTUP, ACTIVE, STARTING_DECOMMISSION, DECOMMISSIONING, DECOMMISSIONED, DEAD, FROZEN |
| user | string | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |
| objectId | string | optional |  |


## <a name="model-SingularityMesosArtifact"></a> SingularityMesosArtifact

| name | type | required | description |
|------|------|----------|-------------|
| cache | boolean | optional |  |
| uri | string | optional |  |
| extract | boolean | optional |  |
| executable | boolean | optional |  |


## <a name="model-SingularityMesosTaskLabel"></a> SingularityMesosTaskLabel

| name | type | required | description |
|------|------|----------|-------------|
| key | string | optional |  |
| value | string | optional |  |


## <a name="model-SingularityPauseRequest"></a> SingularityPauseRequest

| name | type | required | description |
|------|------|----------|-------------|
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| killTasks | boolean | optional | If set to false, tasks will be allowed to finish instead of killed immediately |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |
| runShellCommandBeforeKill | [SingularityShellCommand](#model-SingularityShellCommand) | optional | Attempt to run this shell command on each task before it is shut down |


## <a name="model-SingularityPendingDeploy"></a> SingularityPendingDeploy

| name | type | required | description |
|------|------|----------|-------------|
| currentDeployState | [DeployState](#model-DeployState) | optional |  Allowable values: SUCCEEDED, FAILED_INTERNAL_STATE, CANCELING, WAITING, OVERDUE, FAILED, CANCELED |
| updatedRequest | [SingularityRequest](#model-SingularityRequest) | optional |  |
| deployProgress | [SingularityDeployProgress](#model-SingularityDeployProgress) | optional |  |
| lastLoadBalancerUpdate | [SingularityLoadBalancerUpdate](#model-SingularityLoadBalancerUpdate) | optional |  |
| deployMarker | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |


## <a name="model-SingularityPendingRequest"></a> SingularityPendingRequest

| name | type | required | description |
|------|------|----------|-------------|
| resources | [Resources](#model-Resources) | optional |  |
| runId | string | optional |  |
| skipHealthchecks | boolean | optional |  |
| user | string | optional |  |
| requestId | string | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |
| deployId | string | optional |  |
| runAt | long | optional |  |
| actionId | string | optional |  |
| cmdLineArgsList | Array[string] | optional |  |
| pendingType | [PendingType](#model-PendingType) | optional |  Allowable values: IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, NEXT_DEPLOY_STEP, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP, CANCEL_BOUNCE, TASK_BOUNCE, DEPLOY_CANCELLED, DEPLOY_FAILED |


## <a name="model-SingularityPendingTask"></a> SingularityPendingTask

| name | type | required | description |
|------|------|----------|-------------|
| resources | [Resources](#model-Resources) | optional |  |
| runId | string | optional |  |
| skipHealthchecks | boolean | optional |  |
| pendingTaskId | [SingularityPendingTaskId](#model-SingularityPendingTaskId) | optional |  |
| user | string | optional |  |
| message | string | optional |  |
| actionId | string | optional |  |
| cmdLineArgsList | Array[string] | optional |  |


## <a name="model-SingularityPendingTaskId"></a> SingularityPendingTaskId

| name | type | required | description |
|------|------|----------|-------------|
| nextRunAt | long | optional |  |
| requestId | string | optional |  |
| deployId | string | optional |  |
| pendingType | [PendingType](#model-PendingType) | optional |  Allowable values: IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, NEXT_DEPLOY_STEP, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP, CANCEL_BOUNCE, TASK_BOUNCE, DEPLOY_CANCELLED, DEPLOY_FAILED |
| instanceNo | int | optional |  |
| createdAt | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityPriorityFreeze"></a> SingularityPriorityFreeze

| name | type | required | description |
|------|------|----------|-------------|
| killTasks | boolean | required | If true, kill currently running tasks, and do not launch new tasks below the minimumPriorityLevel. If false, do not launch new tasks below minimumPriorityLevel |
| message | string | optional | An optional message/reason for creating the priority kill |
| actionId | string | optional | A unique ID for this priority kill |
| minimumPriorityLevel | double | required | Kill (if killTasks is true) or do not launch (if killTasks is false) tasks below this priority level |


## <a name="model-SingularityPriorityFreezeParent"></a> SingularityPriorityFreezeParent

| name | type | required | description |
|------|------|----------|-------------|
| priorityFreeze | [SingularityPriorityFreeze](#model-SingularityPriorityFreeze) | optional |  |
| user | string | optional |  |
| timestamp | long | optional |  |


## <a name="model-SingularityRack"></a> SingularityRack

| name | type | required | description |
|------|------|----------|-------------|
| currentState | [SingularityMachineStateHistoryUpdate](#model-SingularityMachineStateHistoryUpdate) | optional |  |
| firstSeenAt | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityRequest"></a> SingularityRequest

| name | type | required | description |
|------|------|----------|-------------|
| hideEvenNumberAcrossRacksHint | boolean | optional |  |
| readOnlyGroups | [Set](#model-Set) | optional | Users in these groups are allowed read only access to this request |
| taskExecutionTimeLimitMillis | long | optional | If set, don't allow any taks for this request to run for longer than this amount of time |
| taskLogErrorRegexCaseSensitive | boolean | optional | Determines if taskLogErrorRegex is case sensitive |
| schedule | string | optional | A schedule in cron, RFC5545, or quartz format |
| skipHealthchecks | boolean | optional | If true, do not run healthchecks |
| waitAtLeastMillisAfterTaskFinishesForReschedule | long | optional | When a scheduled job finishes, wait at least this long before rescheduling it |
| taskPriorityLevel | double | optional | a priority level from 0.0 to 1.0 for all tasks associated with the request |
| emailConfigurationOverrides | [Map[SingularityEmailType,List[SingularityEmailDestination]]](#model-Map[SingularityEmailType,List[SingularityEmailDestination]]) | optional | Overrides for email recipients by email type for this request |
| rackAffinity | Array[string] | optional | If set, prefer this specific rack when launching tasks |
| maxTasksPerOffer | int | optional | Do not schedule more than this many tasks using a single offer from a single mesos slave |
| slavePlacement | [SlavePlacement](#model-SlavePlacement) | optional | Strategy for determining where to place new tasks. Can be SEPARATE, OPTIMISTIC, GREEDY, SEPARATE_BY_DEPLOY, or SEPARATE_BY_REQUEST |
| bounceAfterScale | boolean | optional | Used for SingularityUI. If true, automatically trigger a bounce after changing the request's instance count |
| readWriteGroups | [Set](#model-Set) | optional | Users in these groups are allowed read/write access to this request |
| group | string | optional | Auth group associated with this request. Users in this group are allowed read/write access to this request |
| rackSensitive | boolean | optional | Spread instances for this request evenly across separate racks |
| allowedSlaveAttributes | [Map[string,string]](#model-Map[string,string]) | optional | Allow tasks to run on slaves with these attributes, but do not restrict them to only these slaves |
| owners | Array[string] | optional | A list of emails for the owners of this request |
| requiredRole | string | optional | Mesos Role required for this request. Only offers with the required role will be accepted to execute the tasks associated with the request |
| requestType | [RequestType](#model-RequestType) | required | The type of request, can be SERVICE, WORKER, SCHEDULED, ON_DEMAND, or RUN_ONCE Allowable values: SERVICE, WORKER, SCHEDULED, ON_DEMAND, RUN_ONCE |
| scheduledExpectedRuntimeMillis | long | optional | Expected time for a non-long-running task to run. Singularity will notify owners if a task exceeds this time |
| quartzSchedule | string | optional | A schedule in quartz format |
| requiredSlaveAttributes | [Map[string,string]](#model-Map[string,string]) | optional | Only allow tasks for this request to run on slaves which have these attributes |
| dataCenter | string | optional | the data center associated with this request |
| numRetriesOnFailure | int | optional | For scheduled jobs, retry up to this many times if the job fails |
| loadBalanced | boolean | optional | Indicates that a SERVICE should be load balanced |
| killOldNonLongRunningTasksAfterMillis | long | optional | For non-long-running request types, kill a task after this amount of time if it has been put into CLEANING and has not shut down |
| instances | int | optional | A count of tasks to run for long-running requests |
| scheduleType | [ScheduleType](#model-ScheduleType) | optional | The type of schedule associated with the scheduled field. Can be CRON, QUARTZ, or RFC5545 |
| scheduleTimeZone | string | optional | Time zone to use when running the |
| allowBounceToSameHost | boolean | optional | If set to true, allow tasks to be scheduled on the same host as an existing active task when bouncing |
| taskLogErrorRegex | string | optional | Searching for errors in task logs to include in emails using this regex |
| id | string | required | A unique id for the request |


## <a name="model-SingularityRequestCleanup"></a> SingularityRequestCleanup

| name | type | required | description |
|------|------|----------|-------------|
| removeFromLoadBalancer | boolean | optional |  |
| skipHealthchecks | boolean | optional |  |
| requestId | string | optional |  |
| user | string | optional |  |
| killTasks | boolean | optional |  |
| cleanupType | [RequestCleanupType](#model-RequestCleanupType) | optional |  Allowable values: DELETING, PAUSING, BOUNCE, INCREMENTAL_BOUNCE |
| message | string | optional |  |
| timestamp | long | optional |  |
| deployId | string | optional |  |
| runShellCommandBeforeKill | [SingularityShellCommand](#model-SingularityShellCommand) | optional |  |
| actionId | string | optional |  |


## <a name="model-SingularityRequestDeployState"></a> SingularityRequestDeployState

| name | type | required | description |
|------|------|----------|-------------|
| pendingDeploy | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |
| requestId | string | optional |  |
| activeDeploy | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |


## <a name="model-SingularityRequestGroup"></a> SingularityRequestGroup

| name | type | required | description |
|------|------|----------|-------------|
| metadata | [Map[string,string]](#model-Map[string,string]) | optional |  |
| requestIds | Array[string] | optional |  |
| id | string | optional |  |


## <a name="model-SingularityRequestHistory"></a> SingularityRequestHistory

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| message | string | optional |  |
| request | [SingularityRequest](#model-SingularityRequest) | optional |  |
| eventType | [RequestHistoryType](#model-RequestHistoryType) | optional |  Allowable values: CREATED, UPDATED, DELETING, DELETED, PAUSED, UNPAUSED, ENTERED_COOLDOWN, EXITED_COOLDOWN, FINISHED, DEPLOYED_TO_UNPAUSE, BOUNCED, SCALED, SCALE_REVERTED |
| createdAt | long | optional |  |


## <a name="model-SingularityRequestParent"></a> SingularityRequestParent

| name | type | required | description |
|------|------|----------|-------------|
| expiringSkipHealthchecks | [SingularityExpiringSkipHealthchecks](#model-SingularityExpiringSkipHealthchecks) | optional |  |
| state | [RequestState](#model-RequestState) | optional |  Allowable values: ACTIVE, DELETING, DELETED, PAUSED, SYSTEM_COOLDOWN, FINISHED, DEPLOYING_TO_UNPAUSE |
| pendingDeploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| activeDeploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| expiringPause | [SingularityExpiringPause](#model-SingularityExpiringPause) | optional |  |
| expiringBounce | [SingularityExpiringBounce](#model-SingularityExpiringBounce) | optional |  |
| request | [SingularityRequest](#model-SingularityRequest) | optional |  |
| pendingDeployState | [SingularityPendingDeploy](#model-SingularityPendingDeploy) | optional |  |
| expiringScale | [SingularityExpiringScale](#model-SingularityExpiringScale) | optional |  |
| requestDeployState | [SingularityRequestDeployState](#model-SingularityRequestDeployState) | optional |  |


## <a name="model-SingularityRunNowRequest"></a> SingularityRunNowRequest

| name | type | required | description |
|------|------|----------|-------------|
| resources | [Resources](#model-Resources) | optional | Override the resources from the active deploy for this run |
| runId | string | optional | An id to associate with this request which will be associated with the corresponding launched tasks |
| skipHealthchecks | boolean | optional | If set to true, healthchecks will be skipped for this task run |
| commandLineArgs | Array[string] | optional | Command line arguments to be passed to the task |
| message | string | optional | A message to show to users about why this action was taken |
| runAt | long | optional | Schedule this task to run at a specified time |


## <a name="model-SingularityS3LogMetadata"></a> SingularityS3LogMetadata

| name | type | required | description |
|------|------|----------|-------------|
| key | string | optional | S3 key |
| size | long | optional | File size (in bytes) |
| lastModified | long | optional | Last modified time |
| endTime | long | optional | Time the log file was finished being written to |
| startTime | long | optional | Time the log file started being written to |


## <a name="model-SingularityS3SearchRequest"></a> SingularityS3SearchRequest

| name | type | required | description |
|------|------|----------|-------------|
| requestsAndDeploys | [Map[string,List[string]]](#model-Map[string,List[string]]) | optional | A map of request IDs to a list of deploy ids to search |
| listOnly | boolean | optional | If true, do not generate download/get urls, only list objects |
| maxPerPage | int | optional | Target number of results to return |
| taskIds | Array[string] | optional | A list of task IDs to search for |
| excludeMetadata | boolean | optional | if true, do not query for custom start/end time metadata |
| continuationTokens | [Map[string,ContinuationToken]](#model-Map[string,ContinuationToken]) | optional | S3 continuation tokens, return these to Singularity to continue searching subsequent pages of results |
| end | long | optional | End timestamp (millis, 13 digit) |
| start | long | optional | Start timestamp (millis, 13 digit) |


## <a name="model-SingularityS3SearchResult"></a> SingularityS3SearchResult

| name | type | required | description |
|------|------|----------|-------------|
| results | [Array[SingularityS3LogMetadata]](#model-SingularityS3LogMetadata) | optional | List of S3 log metadata |
| lastPage | boolean | required | If true, there are no further results for any bucket + prefix being searched |
| continuationTokens | [Map[string,ContinuationToken]](#model-Map[string,ContinuationToken]) | optional | S3 continuation tokens, return these to Singularity to continue searching subsequent pages of results |


## <a name="model-SingularitySandbox"></a> SingularitySandbox

| name | type | required | description |
|------|------|----------|-------------|
| slaveHostname | string | optional | Hostname of tasks's slave |
| files | [Array[SingularitySandboxFile]](#model-SingularitySandboxFile) | optional | List of files inside sandbox |
| currentDirectory | string | optional | Current directory |
| fullPathToRoot | string | optional | Full path to the root of the Mesos task sandbox |


## <a name="model-SingularitySandboxFile"></a> SingularitySandboxFile

| name | type | required | description |
|------|------|----------|-------------|
| size | long | optional | File size (in bytes) |
| mode | string | optional | File mode |
| mtime | long | optional | Last modified time |
| name | string | optional | Filename |


## <a name="model-SingularityScaleRequest"></a> SingularityScaleRequest

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | If set to true, healthchecks will be skipped while scaling this request (only) |
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| bounce | boolean | optional | Bounce the request to get to the new scale |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |
| instances | int | optional | The number of instances to scale to |
| incremental | boolean | optional | If present and set to true, old tasks will be killed as soon as replacement tasks are available, instead of waiting for all replacement tasks to be healthy |


## <a name="model-SingularityShellCommand"></a> SingularityShellCommand

| name | type | required | description |
|------|------|----------|-------------|
| logfileName | string | optional | File name for shell command output |
| user | string | optional | User who requested the shell command |
| options | Array[string] | optional | Additional options related to the shell command |
| name | string | required | Name of the shell command to run |


## <a name="model-SingularitySkipHealthchecksRequest"></a> SingularitySkipHealthchecksRequest

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | If set to true, healthchecks will be skipped for all tasks for this request until reversed |
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularitySlave"></a> SingularitySlave

| name | type | required | description |
|------|------|----------|-------------|
| resources | [MesosResourcesObject](#model-MesosResourcesObject) | optional |  |
| currentState | [SingularityMachineStateHistoryUpdate](#model-SingularityMachineStateHistoryUpdate) | optional |  |
| attributes | [Map[string,string]](#model-Map[string,string]) | optional |  |
| host | string | optional | Slave hostname |
| rackId | string | optional | Slave rack ID |
| firstSeenAt | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityState"></a> SingularityState

| name | type | required | description |
|------|------|----------|-------------|
| activeRacks | int | optional |  |
| decomissioningRacks | int | optional |  |
| authDatastoreHealthy | boolean | optional |  |
| generatedAt | long | optional |  |
| activeSlaves | int | optional |  |
| pausedRequests | int | optional |  |
| activeTasks | int | optional |  |
| lbCleanupTasks | int | optional |  |
| overProvisionedRequestIds | Array[string] | optional |  |
| cleaningRequests | int | optional |  |
| deadSlaves | int | optional |  |
| lateTasks | int | optional |  |
| overProvisionedRequests | int | optional |  |
| decommissioningSlaves | int | optional |  |
| unknownRacks | int | optional |  |
| numDeploys | int | optional |  |
| cleaningTasks | int | optional |  |
| launchingTasks | int | optional |  |
| unknownSlaves | int | optional |  |
| oldestDeployStep | long | optional |  |
| activeRequests | int | optional |  |
| futureTasks | int | optional |  |
| lbCleanupRequests | int | optional |  |
| decommissioningRacks | int | optional |  |
| finishedRequests | int | optional |  |
| avgStatusUpdateDelayMs | long | optional |  |
| deadRacks | int | optional |  |
| pendingRequests | int | optional |  |
| maxTaskLag | long | optional |  |
| cooldownRequests | int | optional |  |
| hostStates | [Array[SingularityHostState]](#model-SingularityHostState) | optional |  |
| allRequests | int | optional |  |
| underProvisionedRequests | int | optional |  |
| decomissioningSlaves | int | optional |  |
| oldestDeploy | long | optional |  |
| activeDeploys | [Array[SingularityDeployMarker]](#model-SingularityDeployMarker) | optional |  |
| minimumPriorityLevel | double | optional |  |
| scheduledTasks | int | optional |  |
| underProvisionedRequestIds | Array[string] | optional |  |


## <a name="model-SingularityTask"></a> SingularityTask

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| taskRequest | [SingularityTaskRequest](#model-SingularityTaskRequest) | optional |  |
| rackId | string | optional |  |


## <a name="model-SingularityTaskCleanup"></a> SingularityTaskCleanup

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| removeFromLoadBalancer | boolean | optional |  |
| user | string | optional |  |
| cleanupType | [TaskCleanupType](#model-TaskCleanupType) | optional |  Allowable values: USER_REQUESTED, USER_REQUESTED_TASK_BOUNCE, DECOMISSIONING, SCALING_DOWN, BOUNCING, INCREMENTAL_BOUNCE, DEPLOY_FAILED, NEW_DEPLOY_SUCCEEDED, DEPLOY_STEP_FINISHED, DEPLOY_CANCELED, TASK_EXCEEDED_TIME_LIMIT, UNHEALTHY_NEW_TASK, OVERDUE_NEW_TASK, USER_REQUESTED_DESTROY, INCREMENTAL_DEPLOY_FAILED, INCREMENTAL_DEPLOY_CANCELLED, PRIORITY_KILL, REBALANCE_RACKS, PAUSING, PAUSE, DECOMMISSION_TIMEOUT, REQUEST_DELETING |
| message | string | optional |  |
| runBeforeKillId | [SingularityTaskShellCommandRequestId](#model-SingularityTaskShellCommandRequestId) | optional |  |
| timestamp | long | optional |  |
| actionId | string | optional |  |


## <a name="model-SingularityTaskCredits"></a> SingularityTaskCredits

| name | type | required | description |
|------|------|----------|-------------|
| remaining | int | optional |  |
| enabled | boolean | optional |  |


## <a name="model-SingularityTaskHealthcheckResult"></a> SingularityTaskHealthcheckResult

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| startup | boolean | optional |  |
| durationMillis | long | optional |  |
| errorMessage | string | optional |  |
| statusCode | int | optional |  |
| timestamp | long | optional |  |
| responseBody | string | optional |  |


## <a name="model-SingularityTaskHistory"></a> SingularityTaskHistory

| name | type | required | description |
|------|------|----------|-------------|
| directory | string | optional |  |
| task | [SingularityTask](#model-SingularityTask) | optional |  |
| healthcheckResults | [Array[SingularityTaskHealthcheckResult]](#model-SingularityTaskHealthcheckResult) | optional |  |
| loadBalancerUpdates | [Array[SingularityLoadBalancerUpdate]](#model-SingularityLoadBalancerUpdate) | optional |  |
| taskMetadata | [Array[SingularityTaskMetadata]](#model-SingularityTaskMetadata) | optional |  |
| containerId | string | optional |  |
| shellCommandHistory | [Array[SingularityTaskShellCommandHistory]](#model-SingularityTaskShellCommandHistory) | optional |  |
| taskUpdates | [Array[SingularityTaskHistoryUpdate]](#model-SingularityTaskHistoryUpdate) | optional |  |


## <a name="model-SingularityTaskHistoryUpdate"></a> SingularityTaskHistoryUpdate

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| statusReason | string | optional |  |
| statusMessage | string | optional |  |
| taskState | [ExtendedTaskState](#model-ExtendedTaskState) | optional |  Allowable values: TASK_LAUNCHED, TASK_STAGING, TASK_STARTING, TASK_RUNNING, TASK_CLEANING, TASK_KILLING, TASK_FINISHED, TASK_FAILED, TASK_KILLED, TASK_LOST, TASK_LOST_WHILE_DOWN, TASK_ERROR, TASK_DROPPED, TASK_GONE, TASK_UNREACHABLE, TASK_GONE_BY_OPERATOR, TASK_UNKNOWN |
| timestamp | long | optional |  |
| previous | [Set](#model-Set) | optional |  |


## <a name="model-SingularityTaskId"></a> SingularityTaskId

| name | type | required | description |
|------|------|----------|-------------|
| requestId | string | optional |  |
| host | string | optional |  |
| deployId | string | optional |  |
| sanitizedHost | string | optional |  |
| rackId | string | optional |  |
| sanitizedRackId | string | optional |  |
| instanceNo | int | optional |  |
| startedAt | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityTaskIdHistory"></a> SingularityTaskIdHistory

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| runId | string | optional |  |
| updatedAt | long | optional |  |
| lastTaskState | [ExtendedTaskState](#model-ExtendedTaskState) | optional |  |


## <a name="model-SingularityTaskIdsByStatus"></a> SingularityTaskIdsByStatus

| name | type | required | description |
|------|------|----------|-------------|
| pending | [Array[SingularityPendingTaskId]](#model-SingularityPendingTaskId) | optional |  |
| cleaning | [Array[SingularityTaskId]](#model-SingularityTaskId) | optional |  |
| healthy | [Array[SingularityTaskId]](#model-SingularityTaskId) | optional |  |
| notYetHealthy | [Array[SingularityTaskId]](#model-SingularityTaskId) | optional |  |


## <a name="model-SingularityTaskMetadata"></a> SingularityTaskMetadata

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| level | [MetadataLevel](#model-MetadataLevel) | optional |  Allowable values: INFO, WARN, ERROR |
| type | string | optional |  |
| user | string | optional |  |
| message | string | optional |  |
| title | string | optional |  |
| timestamp | long | optional |  |


## <a name="model-SingularityTaskMetadataRequest"></a> SingularityTaskMetadataRequest

| name | type | required | description |
|------|------|----------|-------------|
| level | [MetadataLevel](#model-MetadataLevel) | optional | Level of metadata, can be INFO, WARN, or ERROR |
| type | string | required | A type to be associated with this metadata |
| message | string | optional | An optional message |
| title | string | required | A title to be associated with this metadata |


## <a name="model-SingularityTaskReconciliationStatistics"></a> SingularityTaskReconciliationStatistics

| name | type | required | description |
|------|------|----------|-------------|
| taskReconciliationResponseP95 | double | optional |  |
| taskReconciliationResponseStddev | double | optional |  |
| taskReconciliationStartedAt | long | optional |  |
| taskReconciliationResponseCount | long | optional |  |
| taskReconciliationResponseP50 | double | optional |  |
| taskReconciliationResponseMean | double | optional |  |
| taskReconciliationResponseMin | long | optional |  |
| taskReconciliationDurationMillis | long | optional |  |
| taskReconciliationIterations | int | optional |  |
| taskReconciliationResponseP75 | double | optional |  |
| taskReconciliationResponseP99 | double | optional |  |
| taskReconciliationResponseMax | long | optional |  |
| taskReconciliationResponseP999 | double | optional |  |
| taskReconciliationResponseP98 | double | optional |  |


## <a name="model-SingularityTaskRequest"></a> SingularityTaskRequest

| name | type | required | description |
|------|------|----------|-------------|
| deploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| request | [SingularityRequest](#model-SingularityRequest) | optional |  |
| pendingTask | [SingularityPendingTask](#model-SingularityPendingTask) | optional |  |


## <a name="model-SingularityTaskShellCommandHistory"></a> SingularityTaskShellCommandHistory

| name | type | required | description |
|------|------|----------|-------------|
| shellRequest | [SingularityTaskShellCommandRequest](#model-SingularityTaskShellCommandRequest) | optional |  |
| shellUpdates | [Array[SingularityTaskShellCommandUpdate]](#model-SingularityTaskShellCommandUpdate) | optional |  |


## <a name="model-SingularityTaskShellCommandRequest"></a> SingularityTaskShellCommandRequest

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| user | string | optional |  |
| timestamp | long | optional |  |
| shellCommand | [SingularityShellCommand](#model-SingularityShellCommand) | optional |  |


## <a name="model-SingularityTaskShellCommandRequestId"></a> SingularityTaskShellCommandRequestId

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| name | string | optional |  |
| timestamp | long | optional |  |


## <a name="model-SingularityTaskShellCommandUpdate"></a> SingularityTaskShellCommandUpdate

| name | type | required | description |
|------|------|----------|-------------|
| updateType | [UpdateType](#model-UpdateType) | optional |  Allowable values: INVALID, ACKED, STARTED, FINISHED, FAILED |
| outputFilename | string | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |
| shellRequestId | [SingularityTaskShellCommandRequestId](#model-SingularityTaskShellCommandRequestId) | optional |  |


## <a name="model-SingularityTaskState"></a> SingularityTaskState

| name | type | required | description |
|------|------|----------|-------------|
| pending | boolean | optional |  |
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| currentState | [ExtendedTaskState](#model-ExtendedTaskState) | optional |  |
| runId | string | optional |  |
| pendingTaskId | [SingularityPendingTaskId](#model-SingularityPendingTaskId) | optional |  |
| taskHistory | [Array[SingularityTaskHistoryUpdate]](#model-SingularityTaskHistoryUpdate) | optional |  |


## <a name="model-SingularityUnpauseRequest"></a> SingularityUnpauseRequest

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | If set to true, instructs new tasks that are scheduled immediately while unpausing to skip healthchecks |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularityUpdatePendingDeployRequest"></a> SingularityUpdatePendingDeployRequest

| name | type | required | description |
|------|------|----------|-------------|
| requestId | string | required | Request id |
| deployId | string | required | Deploy id |
| targetActiveInstances | int | required | Updated target instance count for the active deploy |


## <a name="model-SingularityUser"></a> SingularityUser

| name | type | required | description |
|------|------|----------|-------------|
| authenticated | boolean | optional |  |
| groups | [Set](#model-Set) | optional |  |
| name | string | optional |  |
| email | string | optional |  |
| id | string | optional |  |


## <a name="model-SingularityVolume"></a> SingularityVolume

| name | type | required | description |
|------|------|----------|-------------|
| hostPath | string | optional |  |
| containerPath | string | optional |  |
| mode | [SingularityDockerVolumeMode](#model-SingularityDockerVolumeMode) | optional |  |


## <a name="model-SingularityWebhook"></a> SingularityWebhook

| name | type | required | description |
|------|------|----------|-------------|
| type | [WebhookType](#model-WebhookType) | optional | Webhook type. Allowable values: TASK, REQUEST, DEPLOY |
| uri | string | optional | URI to POST to. |
| user | string | optional | User that created webhook. |
| timestamp | long | optional |  |
| id | string | optional | Unique ID for webhook. |


## <a name="model-SingularityWebhookSummary"></a> SingularityWebhookSummary

| name | type | required | description |
|------|------|----------|-------------|
| webhook | [SingularityWebhook](#model-SingularityWebhook) | optional |  |
| queueSize | int | optional |  |


