# Singularity REST API

Version: 0.7.0

Endpoints:
- [`/api/deploys`](#endpoint-/api/deploys) - Manages Singularity Deploys for existing requests
- [`/api/history`](#endpoint-/api/history) - Manages historical data for tasks, requests, and deploys.
- [`/api/logs`](#endpoint-/api/logs) - Manages Singularity task logs stored in S3.
- [`/api/racks`](#endpoint-/api/racks) - Manages Singularity racks.
- [`/api/requests`](#endpoint-/api/requests) - Manages Singularity Requests, the parent object for any deployed task
- [`/api/sandbox`](#endpoint-/api/sandbox) - Provides a proxy to Mesos sandboxes.
- [`/api/slaves`](#endpoint-/api/slaves) - Manages Singularity slaves.
- [`/api/state`](#endpoint-/api/state) - Provides information about the current state of Singularity.
- [`/api/tasks`](#endpoint-/api/tasks) - Manages Singularity tasks.
- [`/api/test`](#endpoint-/api/test) - Misc testing endpoints.
- [`/api/webhooks`](#endpoint-/api/webhooks) - Manages Singularity webhooks.

Models:
- [`Address`](#model-Address)
- [`AddressOrBuilder`](#model-AddressOrBuilder)
- [`Appc`](#model-Appc)
- [`AppcOrBuilder`](#model-AppcOrBuilder)
- [`ByteString`](#model-ByteString)
- [`CommandInfo`](#model-CommandInfo)
- [`CommandInfoOrBuilder`](#model-CommandInfoOrBuilder)
- [`ContainerInfo`](#model-ContainerInfo)
- [`ContainerInfoOrBuilder`](#model-ContainerInfoOrBuilder)
- [`Credential`](#model-Credential)
- [`CredentialOrBuilder`](#model-CredentialOrBuilder)
- [`Descriptor`](#model-Descriptor)
- [`DiscoveryInfo`](#model-DiscoveryInfo)
- [`DiscoveryInfoOrBuilder`](#model-DiscoveryInfoOrBuilder)
- [`Docker`](#model-Docker)
- [`DockerInfo`](#model-DockerInfo)
- [`DockerInfoOrBuilder`](#model-DockerInfoOrBuilder)
- [`DockerOrBuilder`](#model-DockerOrBuilder)
- [`DurationInfo`](#model-DurationInfo)
- [`DurationInfoOrBuilder`](#model-DurationInfoOrBuilder)
- [`EmbeddedArtifact`](#model-EmbeddedArtifact)
- [`Environment`](#model-Environment)
- [`EnvironmentOrBuilder`](#model-EnvironmentOrBuilder)
- [`ExecutorData`](#model-ExecutorData)
- [`ExecutorID`](#model-ExecutorID)
- [`ExecutorIDOrBuilder`](#model-ExecutorIDOrBuilder)
- [`ExecutorInfo`](#model-ExecutorInfo)
- [`ExecutorInfoOrBuilder`](#model-ExecutorInfoOrBuilder)
- [`ExternalArtifact`](#model-ExternalArtifact)
- [`FileDescriptor`](#model-FileDescriptor)
- [`FileOptions`](#model-FileOptions)
- [`FrameworkID`](#model-FrameworkID)
- [`FrameworkIDOrBuilder`](#model-FrameworkIDOrBuilder)
- [`HTTP`](#model-HTTP)
- [`HTTPOrBuilder`](#model-HTTPOrBuilder)
- [`HealthCheck`](#model-HealthCheck)
- [`HealthCheckOrBuilder`](#model-HealthCheckOrBuilder)
- [`Image`](#model-Image)
- [`ImageOrBuilder`](#model-ImageOrBuilder)
- [`Labels`](#model-Labels)
- [`LabelsOrBuilder`](#model-LabelsOrBuilder)
- [`LoadBalancerRequestId`](#model-LoadBalancerRequestId)
- [`MesosFileChunkObject`](#model-MesosFileChunkObject)
- [`MesosInfo`](#model-MesosInfo)
- [`MesosInfoOrBuilder`](#model-MesosInfoOrBuilder)
- [`MesosTaskStatisticsObject`](#model-MesosTaskStatisticsObject)
- [`MessageOptions`](#model-MessageOptions)
- [`Offer`](#model-Offer)
- [`OfferID`](#model-OfferID)
- [`OfferIDOrBuilder`](#model-OfferIDOrBuilder)
- [`Ports`](#model-Ports)
- [`PortsOrBuilder`](#model-PortsOrBuilder)
- [`Resources`](#model-Resources)
- [`S3Artifact`](#model-S3Artifact)
- [`S3ArtifactSignature`](#model-S3ArtifactSignature)
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
- [`SingularityDockerInfo`](#model-SingularityDockerInfo)
- [`SingularityDockerPortMapping`](#model-SingularityDockerPortMapping)
- [`SingularityExitCooldownRequest`](#model-SingularityExitCooldownRequest)
- [`SingularityExpiringBounce`](#model-SingularityExpiringBounce)
- [`SingularityExpiringPause`](#model-SingularityExpiringPause)
- [`SingularityExpiringScale`](#model-SingularityExpiringScale)
- [`SingularityExpiringSkipHealthchecks`](#model-SingularityExpiringSkipHealthchecks)
- [`SingularityHostState`](#model-SingularityHostState)
- [`SingularityKillTaskRequest`](#model-SingularityKillTaskRequest)
- [`SingularityLoadBalancerUpdate`](#model-SingularityLoadBalancerUpdate)
- [`SingularityMachineChangeRequest`](#model-SingularityMachineChangeRequest)
- [`SingularityMachineStateHistoryUpdate`](#model-SingularityMachineStateHistoryUpdate)
- [`SingularityPauseRequest`](#model-SingularityPauseRequest)
- [`SingularityPendingDeploy`](#model-SingularityPendingDeploy)
- [`SingularityPendingRequest`](#model-SingularityPendingRequest)
- [`SingularityPendingTask`](#model-SingularityPendingTask)
- [`SingularityPendingTaskId`](#model-SingularityPendingTaskId)
- [`SingularityRack`](#model-SingularityRack)
- [`SingularityRequest`](#model-SingularityRequest)
- [`SingularityRequestCleanup`](#model-SingularityRequestCleanup)
- [`SingularityRequestDeployState`](#model-SingularityRequestDeployState)
- [`SingularityRequestHistory`](#model-SingularityRequestHistory)
- [`SingularityRequestParent`](#model-SingularityRequestParent)
- [`SingularityRunNowRequest`](#model-SingularityRunNowRequest)
- [`SingularitySandbox`](#model-SingularitySandbox)
- [`SingularitySandboxFile`](#model-SingularitySandboxFile)
- [`SingularityScaleRequest`](#model-SingularityScaleRequest)
- [`SingularityShellCommand`](#model-SingularityShellCommand)
- [`SingularitySkipHealthchecksRequest`](#model-SingularitySkipHealthchecksRequest)
- [`SingularitySlave`](#model-SingularitySlave)
- [`SingularityState`](#model-SingularityState)
- [`SingularityTask`](#model-SingularityTask)
- [`SingularityTaskCleanup`](#model-SingularityTaskCleanup)
- [`SingularityTaskHealthcheckResult`](#model-SingularityTaskHealthcheckResult)
- [`SingularityTaskHistory`](#model-SingularityTaskHistory)
- [`SingularityTaskHistoryUpdate`](#model-SingularityTaskHistoryUpdate)
- [`SingularityTaskId`](#model-SingularityTaskId)
- [`SingularityTaskIdHistory`](#model-SingularityTaskIdHistory)
- [`SingularityTaskMetadata`](#model-SingularityTaskMetadata)
- [`SingularityTaskMetadataRequest`](#model-SingularityTaskMetadataRequest)
- [`SingularityTaskRequest`](#model-SingularityTaskRequest)
- [`SingularityTaskShellCommandHistory`](#model-SingularityTaskShellCommandHistory)
- [`SingularityTaskShellCommandRequest`](#model-SingularityTaskShellCommandRequest)
- [`SingularityTaskShellCommandRequestId`](#model-SingularityTaskShellCommandRequestId)
- [`SingularityTaskShellCommandUpdate`](#model-SingularityTaskShellCommandUpdate)
- [`SingularityUnpauseRequest`](#model-SingularityUnpauseRequest)
- [`SingularityUpdatePendingDeployRequest`](#model-SingularityUpdatePendingDeployRequest)
- [`SingularityVolume`](#model-SingularityVolume)
- [`SingularityWebhook`](#model-SingularityWebhook)
- [`SingularityWebhookSummary`](#model-SingularityWebhookSummary)
- [`SlaveID`](#model-SlaveID)
- [`SlaveIDOrBuilder`](#model-SlaveIDOrBuilder)
- [`TaskID`](#model-TaskID)
- [`TaskIDOrBuilder`](#model-TaskIDOrBuilder)
- [`TaskInfo`](#model-TaskInfo)
- [`TimeInfo`](#model-TimeInfo)
- [`TimeInfoOrBuilder`](#model-TimeInfoOrBuilder)
- [`URL`](#model-URL)
- [`URLOrBuilder`](#model-URLOrBuilder)
- [`Unavailability`](#model-Unavailability)
- [`UnavailabilityOrBuilder`](#model-UnavailabilityOrBuilder)
- [`UnknownFieldSet`](#model-UnknownFieldSet)

- - -

## Endpoints
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
- No parameters

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
### <a name="endpoint-/api/history"></a> /api/history
#### Overview
Manages historical data for tasks, requests, and deploys.

#### **GET** `/api/history/tasks`

Retrieve the history sorted by startedAt for all inactive tasks.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | false | Optional Request ID to match | string |
| deployId | false | Optional deploy ID to match | string |
| host | false | Optional host to match | string |
| lastTaskStatus | false | Optional last task status to match | string |
| startedAfter | false | Optionally match only tasks started after | long |
| startedBefore | false | Optionally match only tasks started before | long |
| orderDirection | false | Sort direction | string |
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |

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

###### Response
[UNKNOWN[string]](#model-UNKNOWN[string])


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
| host | false | Optional host to match | string |
| lastTaskStatus | false | Optional last task status to match | string |
| startedAfter | false | Optionally match only tasks started after | long |
| startedBefore | false | Optionally match only tasks started before | long |
| orderDirection | false | Sort direction | string |
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |

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

###### Response
[SingularityTaskIdHistory](#model-SingularityTaskIdHistory)


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
| count | false | Naximum number of items to return | int |
| page | false | Which page of items to view | int |

###### Response
[List[SingularityRequestHistory]](#model-SingularityRequestHistory)


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

###### Response
[List[SingularityDeployHistory]](#model-SingularityDeployHistory)


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

###### Response
[SingularityDeployHistory](#model-SingularityDeployHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


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

###### Response
[List[SingularityS3Log]](#model-SingularityS3Log)


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

###### Response
[List[SingularityS3Log]](#model-SingularityS3Log)


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

###### Response
[List[SingularityS3Log]](#model-SingularityS3Log)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
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

###### Response
[List[SingularityMachineStateHistoryUpdate]](#model-SingularityMachineStateHistoryUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/racks/rack/{rackId}`

Remove a known rack, erasing history. This operation will cancel decomissioning of racks


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Rack ID | string |

###### Response



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

###### Response
[List[SingularityRack]](#model-SingularityRack)


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
#### **DELETE** `/api/requests/request/{requestId}/skipHealthchecks`

Delete/cancel the expiring skipHealthchecks. This makes the skipHealthchecks request permanent.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID | string |

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

###### Response
[SingularityTaskId](#model-SingularityTaskId)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/requests/request/{requestId}/run`

Schedule a one-off or scheduled Singularity request for immediate execution.


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
- No parameters

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
- No parameters

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
- No parameters

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
- No parameters

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
- No parameters

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
- No parameters

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
- No parameters

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
- No parameters

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

###### Response



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

###### Response
[List[SingularitySlave]](#model-SingularitySlave)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="endpoint-/api/state"></a> /api/state
#### Overview
Provides information about the current state of Singularity.

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
#### **GET** `/api/tasks/task/{taskId}/cleanup`

Get the cleanup object for the task, if it exists


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |

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
| body | false |  | [SingularityKillTaskRequest](#model-linkType)</a> |

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
- No parameters

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
- No parameters

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
- No parameters

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
- No parameters

###### Response
[UNKNOWN[SingularityKilledTaskIdRecord]](#model-UNKNOWN[SingularityKilledTaskIdRecord])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/cleaning`

Retrieve the list of cleaning tasks.


###### Parameters
- No parameters

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
- No parameters

###### Response
[UNKNOWN[SingularityTask]](#model-UNKNOWN[SingularityTask])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


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
- No parameters

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

###### Response
[List[SingularityDeployUpdate]](#model-SingularityDeployUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks`

Retrieve a list of active webhooks.


###### Parameters
- No parameters

###### Response
[List[SingularityWebhook]](#model-SingularityWebhook)


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
#### **DELETE** `/api/webhooks`

Delete a specific webhook.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | false |  | string |

###### Response
string


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -

## Data Types

## <a name="model-Address"></a> Address

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Address](#model-Address) | optional |  |
| ip | string | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Address&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Address&gt;) | optional |  |
| hostname | string | optional |  |
| ipBytes | [ByteString](#model-ByteString) | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| hostnameBytes | [ByteString](#model-ByteString) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| port | int | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-AddressOrBuilder"></a> AddressOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| ip | string | optional |  |
| ipBytes | [ByteString](#model-ByteString) | optional |  |
| hostname | string | optional |  |
| hostnameBytes | [ByteString](#model-ByteString) | optional |  |
| port | int | optional |  |


## <a name="model-Appc"></a> Appc

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Appc](#model-Appc) | optional |  |
| idBytes | [ByteString](#model-ByteString) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Image$Appc&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Image$Appc&gt;) | optional |  |
| labelsOrBuilder | [LabelsOrBuilder](#model-LabelsOrBuilder) | optional |  |
| labels | [Labels](#model-Labels) | optional |  |
| initialized | boolean | optional |  |
| name | string | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| initializationErrorString | string | optional |  |
| id | string | optional |  |


## <a name="model-AppcOrBuilder"></a> AppcOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| idBytes | [ByteString](#model-ByteString) | optional |  |
| labelsOrBuilder | [LabelsOrBuilder](#model-LabelsOrBuilder) | optional |  |
| labels | [Labels](#model-Labels) | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| name | string | optional |  |
| id | string | optional |  |


## <a name="model-ByteString"></a> ByteString

| name | type | required | description |
|------|------|----------|-------------|
| validUtf8 | boolean | optional |  |
| empty | boolean | optional |  |


## <a name="model-CommandInfo"></a> CommandInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [CommandInfo](#model-CommandInfo) | optional |  |
| urisOrBuilderList | [List[? extends org.apache.mesos.Protos$CommandInfo$URIOrBuilder]](#model-List[? extends org.apache.mesos.Protos$CommandInfo$URIOrBuilder]) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$CommandInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$CommandInfo&gt;) | optional |  |
| argumentsCount | int | optional |  |
| urisCount | int | optional |  |
| argumentsList | Array[string] | optional |  |
| user | string | optional |  |
| value | string | optional |  |
| initialized | boolean | optional |  |
| environment | [Environment](#model-Environment) | optional |  |
| userBytes | [ByteString](#model-ByteString) | optional |  |
| shell | boolean | optional |  |
| serializedSize | int | optional |  |
| urisList | [List[URI]](#model-List[URI]) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| environmentOrBuilder | [EnvironmentOrBuilder](#model-EnvironmentOrBuilder) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-CommandInfoOrBuilder"></a> CommandInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| urisOrBuilderList | [List[? extends org.apache.mesos.Protos$CommandInfo$URIOrBuilder]](#model-List[? extends org.apache.mesos.Protos$CommandInfo$URIOrBuilder]) | optional |  |
| argumentsCount | int | optional |  |
| urisCount | int | optional |  |
| argumentsList | Array[string] | optional |  |
| user | string | optional |  |
| value | string | optional |  |
| userBytes | [ByteString](#model-ByteString) | optional |  |
| environment | [Environment](#model-Environment) | optional |  |
| shell | boolean | optional |  |
| environmentOrBuilder | [EnvironmentOrBuilder](#model-EnvironmentOrBuilder) | optional |  |
| urisList | [List[URI]](#model-List[URI]) | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-ContainerInfo"></a> ContainerInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [ContainerInfo](#model-ContainerInfo) | optional |  |
| networkInfosList | [List[NetworkInfo]](#model-List[NetworkInfo]) | optional |  |
| networkInfosOrBuilderList | [List[? extends org.apache.mesos.Protos$NetworkInfoOrBuilder]](#model-List[? extends org.apache.mesos.Protos$NetworkInfoOrBuilder]) | optional |  |
| type | [Type](#model-Type) | optional |  Allowable values: DOCKER, MESOS |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo&gt;) | optional |  |
| mesos | [MesosInfo](#model-MesosInfo) | optional |  |
| hostname | string | optional |  |
| mesosOrBuilder | [MesosInfoOrBuilder](#model-MesosInfoOrBuilder) | optional |  |
| dockerOrBuilder | [DockerInfoOrBuilder](#model-DockerInfoOrBuilder) | optional |  |
| initialized | boolean | optional |  |
| volumesCount | int | optional |  |
| serializedSize | int | optional |  |
| networkInfosCount | int | optional |  |
| volumesList | [List[Volume]](#model-List[Volume]) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| hostnameBytes | [ByteString](#model-ByteString) | optional |  |
| volumesOrBuilderList | [List[? extends org.apache.mesos.Protos$VolumeOrBuilder]](#model-List[? extends org.apache.mesos.Protos$VolumeOrBuilder]) | optional |  |
| docker | [DockerInfo](#model-DockerInfo) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-ContainerInfoOrBuilder"></a> ContainerInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| networkInfosList | [List[NetworkInfo]](#model-List[NetworkInfo]) | optional |  |
| networkInfosOrBuilderList | [List[? extends org.apache.mesos.Protos$NetworkInfoOrBuilder]](#model-List[? extends org.apache.mesos.Protos$NetworkInfoOrBuilder]) | optional |  |
| type | [Type](#model-Type) | optional |  Allowable values: DOCKER, MESOS |
| mesos | [MesosInfo](#model-MesosInfo) | optional |  |
| hostname | string | optional |  |
| mesosOrBuilder | [MesosInfoOrBuilder](#model-MesosInfoOrBuilder) | optional |  |
| dockerOrBuilder | [DockerInfoOrBuilder](#model-DockerInfoOrBuilder) | optional |  |
| volumesCount | int | optional |  |
| volumesList | [List[Volume]](#model-List[Volume]) | optional |  |
| networkInfosCount | int | optional |  |
| hostnameBytes | [ByteString](#model-ByteString) | optional |  |
| volumesOrBuilderList | [List[? extends org.apache.mesos.Protos$VolumeOrBuilder]](#model-List[? extends org.apache.mesos.Protos$VolumeOrBuilder]) | optional |  |
| docker | [DockerInfo](#model-DockerInfo) | optional |  |


## <a name="model-Credential"></a> Credential

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Credential](#model-Credential) | optional |  |
| secret | string | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Credential&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Credential&gt;) | optional |  |
| initialized | boolean | optional |  |
| principal | string | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| principalBytes | [ByteString](#model-ByteString) | optional |  |
| secretBytes | [ByteString](#model-ByteString) | optional |  |
| initializationErrorString | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-CredentialOrBuilder"></a> CredentialOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| secret | string | optional |  |
| principal | string | optional |  |
| principalBytes | [ByteString](#model-ByteString) | optional |  |
| secretBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-Descriptor"></a> Descriptor

| name | type | required | description |
|------|------|----------|-------------|
| enumTypes | [List[EnumDescriptor]](#model-List[EnumDescriptor]) | optional |  |
| fullName | string | optional |  |
| containingType | [Descriptor](#model-Descriptor) | optional |  |
| file | [FileDescriptor](#model-FileDescriptor) | optional |  |
| extensions | [List[FieldDescriptor]](#model-List[FieldDescriptor]) | optional |  |
| options | [MessageOptions](#model-MessageOptions) | optional |  |
| fields | [List[FieldDescriptor]](#model-List[FieldDescriptor]) | optional |  |
| name | string | optional |  |
| index | int | optional |  |
| nestedTypes | [List[Descriptor]](#model-List[Descriptor]) | optional |  |


## <a name="model-DiscoveryInfo"></a> DiscoveryInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [DiscoveryInfo](#model-DiscoveryInfo) | optional |  |
| location | string | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$DiscoveryInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$DiscoveryInfo&gt;) | optional |  |
| labelsOrBuilder | [LabelsOrBuilder](#model-LabelsOrBuilder) | optional |  |
| versionBytes | [ByteString](#model-ByteString) | optional |  |
| labels | [Labels](#model-Labels) | optional |  |
| locationBytes | [ByteString](#model-ByteString) | optional |  |
| initialized | boolean | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| name | string | optional |  |
| environment | string | optional |  |
| ports | [Ports](#model-Ports) | optional |  |
| visibility | [Visibility](#model-Visibility) | optional |  Allowable values: FRAMEWORK, CLUSTER, EXTERNAL |
| environmentBytes | [ByteString](#model-ByteString) | optional |  |
| serializedSize | int | optional |  |
| portsOrBuilder | [PortsOrBuilder](#model-PortsOrBuilder) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| version | string | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-DiscoveryInfoOrBuilder"></a> DiscoveryInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| location | string | optional |  |
| labelsOrBuilder | [LabelsOrBuilder](#model-LabelsOrBuilder) | optional |  |
| versionBytes | [ByteString](#model-ByteString) | optional |  |
| labels | [Labels](#model-Labels) | optional |  |
| locationBytes | [ByteString](#model-ByteString) | optional |  |
| name | string | optional |  |
| environment | string | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| ports | [Ports](#model-Ports) | optional |  |
| visibility | [Visibility](#model-Visibility) | optional |  Allowable values: FRAMEWORK, CLUSTER, EXTERNAL |
| environmentBytes | [ByteString](#model-ByteString) | optional |  |
| portsOrBuilder | [PortsOrBuilder](#model-PortsOrBuilder) | optional |  |
| version | string | optional |  |


## <a name="model-Docker"></a> Docker

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Docker](#model-Docker) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Image$Docker&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Image$Docker&gt;) | optional |  |
| credentialOrBuilder | [CredentialOrBuilder](#model-CredentialOrBuilder) | optional |  |
| credential | [Credential](#model-Credential) | optional |  |
| initialized | boolean | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| name | string | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-DockerInfo"></a> DockerInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [DockerInfo](#model-DockerInfo) | optional |  |
| portMappingsOrBuilderList | [List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]) | optional |  |
| parametersList | [List[Parameter]](#model-List[Parameter]) | optional |  |
| parametersOrBuilderList | [List[? extends org.apache.mesos.Protos$ParameterOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ParameterOrBuilder]) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo$DockerInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo$DockerInfo&gt;) | optional |  |
| volumeDriver | string | optional |  |
| volumeDriverBytes | [ByteString](#model-ByteString) | optional |  |
| forcePullImage | boolean | optional |  |
| imageBytes | [ByteString](#model-ByteString) | optional |  |
| initialized | boolean | optional |  |
| privileged | boolean | optional |  |
| portMappingsCount | int | optional |  |
| parametersCount | int | optional |  |
| serializedSize | int | optional |  |
| network | [Network](#model-Network) | optional |  Allowable values: HOST, BRIDGE, NONE |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| portMappingsList | [List[PortMapping]](#model-List[PortMapping]) | optional |  |
| image | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-DockerInfoOrBuilder"></a> DockerInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| portMappingsOrBuilderList | [List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]) | optional |  |
| parametersList | [List[Parameter]](#model-List[Parameter]) | optional |  |
| parametersOrBuilderList | [List[? extends org.apache.mesos.Protos$ParameterOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ParameterOrBuilder]) | optional |  |
| volumeDriver | string | optional |  |
| volumeDriverBytes | [ByteString](#model-ByteString) | optional |  |
| forcePullImage | boolean | optional |  |
| imageBytes | [ByteString](#model-ByteString) | optional |  |
| privileged | boolean | optional |  |
| portMappingsCount | int | optional |  |
| parametersCount | int | optional |  |
| network | [Network](#model-Network) | optional |  Allowable values: HOST, BRIDGE, NONE |
| portMappingsList | [List[PortMapping]](#model-List[PortMapping]) | optional |  |
| image | string | optional |  |


## <a name="model-DockerOrBuilder"></a> DockerOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| credentialOrBuilder | [CredentialOrBuilder](#model-CredentialOrBuilder) | optional |  |
| credential | [Credential](#model-Credential) | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| name | string | optional |  |


## <a name="model-DurationInfo"></a> DurationInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [DurationInfo](#model-DurationInfo) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$DurationInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$DurationInfo&gt;) | optional |  |
| nanoseconds | long | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-DurationInfoOrBuilder"></a> DurationInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| nanoseconds | long | optional |  |


## <a name="model-EmbeddedArtifact"></a> EmbeddedArtifact

| name | type | required | description |
|------|------|----------|-------------|
| targetFolderRelativeToTask | string | optional |  |
| md5sum | string | optional |  |
| filename | string | optional |  |
| name | string | optional |  |
| content | [Array[byte]](#model-byte) | optional |  |


## <a name="model-Environment"></a> Environment

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Environment](#model-Environment) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Environment&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Environment&gt;) | optional |  |
| initialized | boolean | optional |  |
| variablesCount | int | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| variablesOrBuilderList | [List[? extends org.apache.mesos.Protos$Environment$VariableOrBuilder]](#model-List[? extends org.apache.mesos.Protos$Environment$VariableOrBuilder]) | optional |  |
| variablesList | [List[Variable]](#model-List[Variable]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-EnvironmentOrBuilder"></a> EnvironmentOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| variablesCount | int | optional |  |
| variablesOrBuilderList | [List[? extends org.apache.mesos.Protos$Environment$VariableOrBuilder]](#model-List[? extends org.apache.mesos.Protos$Environment$VariableOrBuilder]) | optional |  |
| variablesList | [List[Variable]](#model-List[Variable]) | optional |  |


## <a name="model-ExecutorData"></a> ExecutorData

| name | type | required | description |
|------|------|----------|-------------|
| skipLogrotateAndCompress | boolean | optional |  |
| loggingExtraFields | [Map[string,string]](#model-Map[string,string]) | optional |  |
| embeddedArtifacts | [Array[EmbeddedArtifact]](#model-EmbeddedArtifact) | optional |  |
| s3Artifacts | [Array[S3Artifact]](#model-S3Artifact) | optional |  |
| successfulExitCodes | Array[int] | optional |  |
| runningSentinel | string | optional |  |
| maxOpenFiles | int | optional |  |
| externalArtifacts | [Array[ExternalArtifact]](#model-ExternalArtifact) | optional |  |
| user | string | optional |  |
| preserveTaskSandboxAfterFinish | boolean | optional |  |
| extraCmdLineArgs | Array[string] | optional |  |
| loggingTag | string | optional |  |
| loggingS3Bucket | string | optional |  |
| sigKillProcessesAfterMillis | long | optional |  |
| maxTaskThreads | int | optional |  |
| s3ArtifactSignatures | [Array[S3ArtifactSignature]](#model-S3ArtifactSignature) | optional |  |
| cmd | string | optional |  |


## <a name="model-ExecutorID"></a> ExecutorID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [ExecutorID](#model-ExecutorID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorID&gt;) | optional |  |
| initialized | boolean | optional |  |
| value | string | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-ExecutorIDOrBuilder"></a> ExecutorIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-ExecutorInfo"></a> ExecutorInfo

| name | type | required | description |
|------|------|----------|-------------|
| commandOrBuilder | [CommandInfoOrBuilder](#model-CommandInfoOrBuilder) | optional |  |
| defaultInstanceForType | [ExecutorInfo](#model-ExecutorInfo) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorInfo&gt;) | optional |  |
| resourcesOrBuilderList | [List[? extends org.apache.mesos.Protos$ResourceOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ResourceOrBuilder]) | optional |  |
| data | [ByteString](#model-ByteString) | optional |  |
| source | string | optional |  |
| containerOrBuilder | [ContainerInfoOrBuilder](#model-ContainerInfoOrBuilder) | optional |  |
| executorId | [ExecutorID](#model-ExecutorID) | optional |  |
| container | [ContainerInfo](#model-ContainerInfo) | optional |  |
| initialized | boolean | optional |  |
| name | string | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| sourceBytes | [ByteString](#model-ByteString) | optional |  |
| frameworkId | [FrameworkID](#model-FrameworkID) | optional |  |
| command | [CommandInfo](#model-CommandInfo) | optional |  |
| frameworkIdOrBuilder | [FrameworkIDOrBuilder](#model-FrameworkIDOrBuilder) | optional |  |
| executorIdOrBuilder | [ExecutorIDOrBuilder](#model-ExecutorIDOrBuilder) | optional |  |
| serializedSize | int | optional |  |
| resourcesList | [List[Resource]](#model-List[Resource]) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| discovery | [DiscoveryInfo](#model-DiscoveryInfo) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| resourcesCount | int | optional |  |
| initializationErrorString | string | optional |  |
| discoveryOrBuilder | [DiscoveryInfoOrBuilder](#model-DiscoveryInfoOrBuilder) | optional |  |


## <a name="model-ExecutorInfoOrBuilder"></a> ExecutorInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| commandOrBuilder | [CommandInfoOrBuilder](#model-CommandInfoOrBuilder) | optional |  |
| resourcesOrBuilderList | [List[? extends org.apache.mesos.Protos$ResourceOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ResourceOrBuilder]) | optional |  |
| data | [ByteString](#model-ByteString) | optional |  |
| source | string | optional |  |
| containerOrBuilder | [ContainerInfoOrBuilder](#model-ContainerInfoOrBuilder) | optional |  |
| executorId | [ExecutorID](#model-ExecutorID) | optional |  |
| container | [ContainerInfo](#model-ContainerInfo) | optional |  |
| name | string | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| command | [CommandInfo](#model-CommandInfo) | optional |  |
| frameworkId | [FrameworkID](#model-FrameworkID) | optional |  |
| sourceBytes | [ByteString](#model-ByteString) | optional |  |
| frameworkIdOrBuilder | [FrameworkIDOrBuilder](#model-FrameworkIDOrBuilder) | optional |  |
| executorIdOrBuilder | [ExecutorIDOrBuilder](#model-ExecutorIDOrBuilder) | optional |  |
| resourcesList | [List[Resource]](#model-List[Resource]) | optional |  |
| discovery | [DiscoveryInfo](#model-DiscoveryInfo) | optional |  |
| resourcesCount | int | optional |  |
| discoveryOrBuilder | [DiscoveryInfoOrBuilder](#model-DiscoveryInfoOrBuilder) | optional |  |


## <a name="model-ExternalArtifact"></a> ExternalArtifact

| name | type | required | description |
|------|------|----------|-------------|
| targetFolderRelativeToTask | string | optional |  |
| md5sum | string | optional |  |
| url | string | optional |  |
| filename | string | optional |  |
| filesize | long | optional |  |
| name | string | optional |  |


## <a name="model-FileDescriptor"></a> FileDescriptor

| name | type | required | description |
|------|------|----------|-------------|
| enumTypes | [List[EnumDescriptor]](#model-List[EnumDescriptor]) | optional |  |
| publicDependencies | [List[FileDescriptor]](#model-List[FileDescriptor]) | optional |  |
| extensions | [List[FieldDescriptor]](#model-List[FieldDescriptor]) | optional |  |
| services | [List[ServiceDescriptor]](#model-List[ServiceDescriptor]) | optional |  |
| options | [FileOptions](#model-FileOptions) | optional |  |
| messageTypes | [List[Descriptor]](#model-List[Descriptor]) | optional |  |
| name | string | optional |  |
| dependencies | [List[FileDescriptor]](#model-List[FileDescriptor]) | optional |  |
| package | string | optional |  |


## <a name="model-FileOptions"></a> FileOptions

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [FileOptions](#model-FileOptions) | optional |  |
| javaMultipleFiles | boolean | optional |  |
| optimizeFor | [OptimizeMode](#model-OptimizeMode) | optional |  Allowable values: SPEED, CODE_SIZE, LITE_RUNTIME |
| javaPackageBytes | [ByteString](#model-ByteString) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$FileOptions&gt;](#model-com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$FileOptions&gt;) | optional |  |
| goPackageBytes | [ByteString](#model-ByteString) | optional |  |
| javaGenericServices | boolean | optional |  |
| uninterpretedOptionCount | int | optional |  |
| javaOuterClassnameBytes | [ByteString](#model-ByteString) | optional |  |
| initialized | boolean | optional |  |
| javaOuterClassname | string | optional |  |
| pyGenericServices | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| javaPackage | string | optional |  |
| uninterpretedOptionList | [List[UninterpretedOption]](#model-List[UninterpretedOption]) | optional |  |
| goPackage | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| uninterpretedOptionOrBuilderList | [List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]](#model-List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]) | optional |  |
| javaGenerateEqualsAndHash | boolean | optional |  |
| initializationErrorString | string | optional |  |
| ccGenericServices | boolean | optional |  |


## <a name="model-FrameworkID"></a> FrameworkID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [FrameworkID](#model-FrameworkID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$FrameworkID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$FrameworkID&gt;) | optional |  |
| initialized | boolean | optional |  |
| value | string | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-FrameworkIDOrBuilder"></a> FrameworkIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-HTTP"></a> HTTP

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [HTTP](#model-HTTP) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$HealthCheck$HTTP&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$HealthCheck$HTTP&gt;) | optional |  |
| pathBytes | [ByteString](#model-ByteString) | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| statusesCount | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| path | string | optional |  |
| port | int | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| statusesList | Array[int] | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-HTTPOrBuilder"></a> HTTPOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| pathBytes | [ByteString](#model-ByteString) | optional |  |
| statusesCount | int | optional |  |
| port | int | optional |  |
| path | string | optional |  |
| statusesList | Array[int] | optional |  |


## <a name="model-HealthCheck"></a> HealthCheck

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [HealthCheck](#model-HealthCheck) | optional |  |
| commandOrBuilder | [CommandInfoOrBuilder](#model-CommandInfoOrBuilder) | optional |  |
| gracePeriodSeconds | double | optional |  |
| httpOrBuilder | [HTTPOrBuilder](#model-HTTPOrBuilder) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$HealthCheck&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$HealthCheck&gt;) | optional |  |
| consecutiveFailures | int | optional |  |
| intervalSeconds | double | optional |  |
| initialized | boolean | optional |  |
| command | [CommandInfo](#model-CommandInfo) | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| timeoutSeconds | double | optional |  |
| http | [HTTP](#model-HTTP) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| delaySeconds | double | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-HealthCheckOrBuilder"></a> HealthCheckOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| gracePeriodSeconds | double | optional |  |
| commandOrBuilder | [CommandInfoOrBuilder](#model-CommandInfoOrBuilder) | optional |  |
| httpOrBuilder | [HTTPOrBuilder](#model-HTTPOrBuilder) | optional |  |
| consecutiveFailures | int | optional |  |
| intervalSeconds | double | optional |  |
| command | [CommandInfo](#model-CommandInfo) | optional |  |
| timeoutSeconds | double | optional |  |
| http | [HTTP](#model-HTTP) | optional |  |
| delaySeconds | double | optional |  |


## <a name="model-Image"></a> Image

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Image](#model-Image) | optional |  |
| appcOrBuilder | [AppcOrBuilder](#model-AppcOrBuilder) | optional |  |
| type | [Type](#model-Type) | optional |  Allowable values: APPC, DOCKER |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Image&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Image&gt;) | optional |  |
| dockerOrBuilder | [DockerOrBuilder](#model-DockerOrBuilder) | optional |  |
| initialized | boolean | optional |  |
| appc | [Appc](#model-Appc) | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| docker | [Docker](#model-Docker) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-ImageOrBuilder"></a> ImageOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| appcOrBuilder | [AppcOrBuilder](#model-AppcOrBuilder) | optional |  |
| type | [Type](#model-Type) | optional |  Allowable values: APPC, DOCKER |
| dockerOrBuilder | [DockerOrBuilder](#model-DockerOrBuilder) | optional |  |
| appc | [Appc](#model-Appc) | optional |  |
| docker | [Docker](#model-Docker) | optional |  |


## <a name="model-Labels"></a> Labels

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Labels](#model-Labels) | optional |  |
| labelsList | [List[Label]](#model-List[Label]) | optional |  |
| labelsOrBuilderList | [List[? extends org.apache.mesos.Protos$LabelOrBuilder]](#model-List[? extends org.apache.mesos.Protos$LabelOrBuilder]) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Labels&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Labels&gt;) | optional |  |
| initialized | boolean | optional |  |
| labelsCount | int | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-LabelsOrBuilder"></a> LabelsOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| labelsList | [List[Label]](#model-List[Label]) | optional |  |
| labelsOrBuilderList | [List[? extends org.apache.mesos.Protos$LabelOrBuilder]](#model-List[? extends org.apache.mesos.Protos$LabelOrBuilder]) | optional |  |
| labelsCount | int | optional |  |


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


## <a name="model-MesosInfo"></a> MesosInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [MesosInfo](#model-MesosInfo) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo$MesosInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo$MesosInfo&gt;) | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| image | [Image](#model-Image) | optional |  |
| initializationErrorString | string | optional |  |
| imageOrBuilder | [ImageOrBuilder](#model-ImageOrBuilder) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-MesosInfoOrBuilder"></a> MesosInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| image | [Image](#model-Image) | optional |  |
| imageOrBuilder | [ImageOrBuilder](#model-ImageOrBuilder) | optional |  |


## <a name="model-MesosTaskStatisticsObject"></a> MesosTaskStatisticsObject

| name | type | required | description |
|------|------|----------|-------------|
| memFileBytes | long | optional |  |
| memLimitBytes | long | optional |  |
| cpusThrottledTimeSecs | double | optional |  |
| cpusSystemTimeSecs | double | optional |  |
| memRssBytes | long | optional |  |
| memAnonBytes | long | optional |  |
| memMappedFileBytes | long | optional |  |
| cpusLimit | int | optional |  |
| timestamp | double | optional |  |
| cpusNrPeriods | long | optional |  |
| cpusUserTimeSecs | double | optional |  |
| cpusNrThrottled | long | optional |  |


## <a name="model-MessageOptions"></a> MessageOptions

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [MessageOptions](#model-MessageOptions) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$MessageOptions&gt;](#model-com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$MessageOptions&gt;) | optional |  |
| uninterpretedOptionCount | int | optional |  |
| initialized | boolean | optional |  |
| noStandardDescriptorAccessor | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| messageSetWireFormat | boolean | optional |  |
| uninterpretedOptionList | [List[UninterpretedOption]](#model-List[UninterpretedOption]) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| uninterpretedOptionOrBuilderList | [List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]](#model-List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-Offer"></a> Offer

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Offer](#model-Offer) | optional |  |
| executorIdsOrBuilderList | [List[? extends org.apache.mesos.Protos$ExecutorIDOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ExecutorIDOrBuilder]) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Offer&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Offer&gt;) | optional |  |
| slaveIdOrBuilder | [SlaveIDOrBuilder](#model-SlaveIDOrBuilder) | optional |  |
| urlOrBuilder | [URLOrBuilder](#model-URLOrBuilder) | optional |  |
| executorIdsCount | int | optional |  |
| resourcesOrBuilderList | [List[? extends org.apache.mesos.Protos$ResourceOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ResourceOrBuilder]) | optional |  |
| unavailabilityOrBuilder | [UnavailabilityOrBuilder](#model-UnavailabilityOrBuilder) | optional |  |
| url | [URL](#model-URL) | optional |  |
| unavailability | [Unavailability](#model-Unavailability) | optional |  |
| executorIdsList | [List[ExecutorID]](#model-List[ExecutorID]) | optional |  |
| hostname | string | optional |  |
| attributesCount | int | optional |  |
| initialized | boolean | optional |  |
| attributesList | [List[Attribute]](#model-List[Attribute]) | optional |  |
| idOrBuilder | [OfferIDOrBuilder](#model-OfferIDOrBuilder) | optional |  |
| frameworkId | [FrameworkID](#model-FrameworkID) | optional |  |
| frameworkIdOrBuilder | [FrameworkIDOrBuilder](#model-FrameworkIDOrBuilder) | optional |  |
| serializedSize | int | optional |  |
| resourcesList | [List[Resource]](#model-List[Resource]) | optional |  |
| slaveId | [SlaveID](#model-SlaveID) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| hostnameBytes | [ByteString](#model-ByteString) | optional |  |
| attributesOrBuilderList | [List[? extends org.apache.mesos.Protos$AttributeOrBuilder]](#model-List[? extends org.apache.mesos.Protos$AttributeOrBuilder]) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| resourcesCount | int | optional |  |
| initializationErrorString | string | optional |  |
| id | [OfferID](#model-OfferID) | optional |  |


## <a name="model-OfferID"></a> OfferID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [OfferID](#model-OfferID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$OfferID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$OfferID&gt;) | optional |  |
| initialized | boolean | optional |  |
| value | string | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-OfferIDOrBuilder"></a> OfferIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-Ports"></a> Ports

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Ports](#model-Ports) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Ports&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Ports&gt;) | optional |  |
| initialized | boolean | optional |  |
| portsCount | int | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| portsOrBuilderList | [List[? extends org.apache.mesos.Protos$PortOrBuilder]](#model-List[? extends org.apache.mesos.Protos$PortOrBuilder]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| portsList | [List[Port]](#model-List[Port]) | optional |  |
| initializationErrorString | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-PortsOrBuilder"></a> PortsOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| portsCount | int | optional |  |
| portsOrBuilderList | [List[? extends org.apache.mesos.Protos$PortOrBuilder]](#model-List[? extends org.apache.mesos.Protos$PortOrBuilder]) | optional |  |
| portsList | [List[Port]](#model-List[Port]) | optional |  |


## <a name="model-Resources"></a> Resources

| name | type | required | description |
|------|------|----------|-------------|
| numPorts | int | optional |  |
| memoryMb | double | optional |  |
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


## <a name="model-SingularityBounceRequest"></a> SingularityBounceRequest

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | Instruct replacement tasks for this bounce only to skip healthchecks |
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |
| incremental | boolean | optional | If present and set to true, old tasks will be killed as soon as replacement tasks are available, instead of waiting for all replacement tasks to be healthy |


## <a name="model-SingularityContainerInfo"></a> SingularityContainerInfo

| name | type | required | description |
|------|------|----------|-------------|
| type | [SingularityContainerType](#model-SingularityContainerType) | optional |  Allowable values: MESOS, DOCKER |
| volumes | [Array[SingularityVolume]](#model-SingularityVolume) | optional |  |
| docker | [SingularityDockerInfo](#model-SingularityDockerInfo) | optional |  |


## <a name="model-SingularityDeleteRequestRequest"></a> SingularityDeleteRequestRequest

| name | type | required | description |
|------|------|----------|-------------|
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularityDeploy"></a> SingularityDeploy

| name | type | required | description |
|------|------|----------|-------------|
| customExecutorId | string | optional | Custom Mesos executor id. |
| resources | [com.hubspot.mesos.Resources](#model-com.hubspot.mesos.Resources) | optional | Resources required for this deploy. |
| uris | Array[string] | optional | List of URIs to download before executing the deploy command. |
| containerInfo | [SingularityContainerInfo](#model-SingularityContainerInfo) | optional | Container information for deployment into a container. |
| loadBalancerDomains | [Set](#model-Set) | optional | List of domains to host this service on, for use with the load balancer api |
| arguments | Array[string] | optional | Command arguments. |
| autoAdvanceDeploySteps | boolean | optional | automatically advance to the next target instance count after `deployStepWaitTimeMs` seconds |
| serviceBasePath | string | optional | The base path for the API exposed by the deploy. Used in conjunction with the Load balancer API. |
| customExecutorUser | string | optional | User to run custom executor as |
| customExecutorSource | string | optional | Custom Mesos executor source. |
| metadata | [Map[string,string]](#model-Map[string,string]) | optional | Map of metadata key/value pairs associated with the deployment. |
| healthcheckTimeoutSeconds | long | optional | Single healthcheck HTTP timeout in seconds. |
| healthcheckMaxRetries | int | optional | Maximum number of times to retry an individual healthcheck before failing the deploy. |
| healthcheckPortIndex | int | optional | Perform healthcheck on this dynamically allocated port (e.g. 0 for first port), defaults to first port |
| healthcheckProtocol | [HealthcheckProtocol](#model-HealthcheckProtocol) | optional | Healthcheck protocol - HTTP or HTTPS |
| healthcheckMaxTotalTimeoutSeconds | long | optional | Maximum amount of time to wait before failing a deploy for healthchecks to pass. |
| labels | [Map[string,string]](#model-Map[string,string]) | optional | Labels for tasks associated with this deploy |
| healthcheckUri | string | optional | Deployment Healthcheck URI, if specified will be called after TASK_RUNNING. |
| requestId | string | required | Singularity Request Id which is associated with this deploy. |
| loadBalancerGroups | [Set](#model-Set) | optional | List of load balancer groups associated with this deployment. |
| deployStepWaitTimeMs | int | optional | wait this long between deploy steps |
| skipHealthchecksOnDeploy | boolean | optional | Allows skipping of health checks when deploying. |
| healthcheckIntervalSeconds | long | optional | Time to wait after a failed healthcheck to try again in seconds. |
| command | string | optional | Command to execute for this deployment. |
| executorData | [ExecutorData](#model-ExecutorData) | optional | Executor specific information |
| loadBalancerAdditionalRoutes | Array[string] | optional | Additional routes besides serviceBasePath used by this service |
| timestamp | long | optional | Deploy timestamp. |
| deployInstanceCountPerStep | int | optional | deploy this many instances at a time |
| considerHealthyAfterRunningForSeconds | long | optional | Number of seconds that a service must be healthy to consider the deployment to be successful. |
| loadBalancerOptions | [Map[string,Object]](#model-Map[string,Object]) | optional | Map (Key/Value) of options for the load balancer. |
| maxTaskRetries | int | optional | allowed at most this many failed tasks to be retried before failing the deploy |
| loadBalancerPortIndex | int | optional | Send this port to the load balancer api (e.g. 0 for first port), defaults to first port |
| loadBalancerTemplate | string | optional | Name of load balancer template to use if not using the default template |
| customExecutorCmd | string | optional | Custom Mesos executor |
| env | [Map[string,string]](#model-Map[string,string]) | optional | Map of environment variable definitions. |
| customExecutorResources | [Resources](#model-Resources) | optional | Resources to allocate for custom mesos executor |
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
| targetActiveInstances | int | optional |  |


## <a name="model-SingularityDeployRequest"></a> SingularityDeployRequest

| name | type | required | description |
|------|------|----------|-------------|
| unpauseOnSuccessfulDeploy | boolean | optional | If deploy is successful, also unpause the request |
| deploy | [SingularityDeploy](#model-SingularityDeploy) | required | The Singularity deploy object, containing all the required details about the Deploy |
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


## <a name="model-SingularityDockerInfo"></a> SingularityDockerInfo

| name | type | required | description |
|------|------|----------|-------------|
| parameters | [Map[string,string]](#model-Map[string,string]) | optional |  |
| forcePullImage | boolean | optional |  |
| privileged | boolean | optional |  |
| network | [SingularityDockerNetworkType](#model-SingularityDockerNetworkType) | optional |  |
| portMappings | [Array[SingularityDockerPortMapping]](#model-SingularityDockerPortMapping) | optional |  |
| image | string | optional |  |


## <a name="model-SingularityDockerPortMapping"></a> SingularityDockerPortMapping

| name | type | required | description |
|------|------|----------|-------------|
| hostPort | int | optional |  |
| containerPort | int | optional |  |
| containerPortType | [SingularityPortMappingType](#model-SingularityPortMappingType) | optional |  Allowable values: LITERAL, FROM_OFFER |
| protocol | string | optional |  |
| hostPortType | [SingularityPortMappingType](#model-SingularityPortMappingType) | optional |  Allowable values: LITERAL, FROM_OFFER |


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
| hostAddress | string | optional |  |
| hostname | string | optional |  |
| mesosConnected | boolean | optional |  |
| driverStatus | string | optional |  |
| master | boolean | optional |  |
| mesosMaster | string | optional |  |
| uptime | long | optional |  |
| millisSinceLastOffer | long | optional |  |


## <a name="model-SingularityKillTaskRequest"></a> SingularityKillTaskRequest

| name | type | required | description |
|------|------|----------|-------------|
| waitForReplacementTask | boolean | optional | If set to true, treats this task kill as a bounce - launching another task and waiting for it to become healthy |
| override | boolean | optional | If set to true, instructs the executor to attempt to immediately kill the task, rather than waiting gracefully |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


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
| message | string | optional | A message to show to users about why this action was taken |


## <a name="model-SingularityMachineStateHistoryUpdate"></a> SingularityMachineStateHistoryUpdate

| name | type | required | description |
|------|------|----------|-------------|
| state | [MachineState](#model-MachineState) | optional |  Allowable values: MISSING_ON_STARTUP, ACTIVE, STARTING_DECOMMISSION, DECOMMISSIONING, DECOMMISSIONED, DEAD, FROZEN |
| user | string | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |
| objectId | string | optional |  |


## <a name="model-SingularityPauseRequest"></a> SingularityPauseRequest

| name | type | required | description |
|------|------|----------|-------------|
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| killTasks | boolean | optional | If set to false, tasks will be allowed to finish instead of killed immediately |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularityPendingDeploy"></a> SingularityPendingDeploy

| name | type | required | description |
|------|------|----------|-------------|
| currentDeployState | [DeployState](#model-DeployState) | optional |  Allowable values: SUCCEEDED, FAILED_INTERNAL_STATE, CANCELING, WAITING, OVERDUE, FAILED, CANCELED |
| deployProgress | [SingularityDeployProgress](#model-SingularityDeployProgress) | optional |  |
| lastLoadBalancerUpdate | [SingularityLoadBalancerUpdate](#model-SingularityLoadBalancerUpdate) | optional |  |
| deployMarker | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |


## <a name="model-SingularityPendingRequest"></a> SingularityPendingRequest

| name | type | required | description |
|------|------|----------|-------------|
| runId | string | optional |  |
| skipHealthchecks | boolean | optional |  |
| user | string | optional |  |
| requestId | string | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |
| deployId | string | optional |  |
| actionId | string | optional |  |
| cmdLineArgsList | Array[string] | optional |  |
| pendingType | [PendingType](#model-PendingType) | optional |  Allowable values: IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, NEXT_DEPLOY_STEP, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP, CANCEL_BOUNCE, TASK_BOUNCE, DEPLOY_CANCELLED |


## <a name="model-SingularityPendingTask"></a> SingularityPendingTask

| name | type | required | description |
|------|------|----------|-------------|
| runId | string | optional |  |
| skipHealthchecks | boolean | optional |  |
| pendingTaskId | [SingularityPendingTaskId](#model-SingularityPendingTaskId) | optional |  |
| user | string | optional |  |
| message | string | optional |  |
| cmdLineArgsList | Array[string] | optional |  |


## <a name="model-SingularityPendingTaskId"></a> SingularityPendingTaskId

| name | type | required | description |
|------|------|----------|-------------|
| nextRunAt | long | optional |  |
| requestId | string | optional |  |
| deployId | string | optional |  |
| pendingType | [PendingType](#model-PendingType) | optional |  Allowable values: IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, NEXT_DEPLOY_STEP, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP, CANCEL_BOUNCE, TASK_BOUNCE, DEPLOY_CANCELLED |
| instanceNo | int | optional |  |
| createdAt | long | optional |  |
| id | string | optional |  |


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
| readOnlyGroups | [Set](#model-Set) | optional |  |
| schedule | string | optional |  |
| taskLogErrorRegexCaseSensitive | boolean | optional |  |
| skipHealthchecks | boolean | optional |  |
| waitAtLeastMillisAfterTaskFinishesForReschedule | long | optional |  |
| rackAffinity | Array[string] | optional |  |
| emailConfigurationOverrides | [Map[SingularityEmailType,List[SingularityEmailDestination]]](#model-Map[SingularityEmailType,List[SingularityEmailDestination]]) | optional |  |
| slavePlacement | [SlavePlacement](#model-SlavePlacement) | optional |  |
| bounceAfterScale | boolean | optional |  |
| group | string | optional |  |
| rackSensitive | boolean | optional |  |
| allowedSlaveAttributes | [Map[string,string]](#model-Map[string,string]) | optional |  |
| owners | Array[string] | optional |  |
| requestType | [RequestType](#model-RequestType) | optional |  Allowable values: SERVICE, WORKER, SCHEDULED, ON_DEMAND, RUN_ONCE |
| scheduledExpectedRuntimeMillis | long | optional |  |
| quartzSchedule | string | optional |  |
| requiredSlaveAttributes | [Map[string,string]](#model-Map[string,string]) | optional |  |
| numRetriesOnFailure | int | optional |  |
| loadBalanced | boolean | optional |  |
| killOldNonLongRunningTasksAfterMillis | long | optional |  |
| instances | int | optional |  |
| scheduleType | [ScheduleType](#model-ScheduleType) | optional |  |
| taskLogErrorRegex | string | optional |  |
| id | string | optional |  |


## <a name="model-SingularityRequestCleanup"></a> SingularityRequestCleanup

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional |  |
| requestId | string | optional |  |
| user | string | optional |  |
| killTasks | boolean | optional |  |
| cleanupType | [RequestCleanupType](#model-RequestCleanupType) | optional |  Allowable values: DELETING, PAUSING, BOUNCE, INCREMENTAL_BOUNCE |
| message | string | optional |  |
| timestamp | long | optional |  |
| deployId | string | optional |  |
| actionId | string | optional |  |


## <a name="model-SingularityRequestDeployState"></a> SingularityRequestDeployState

| name | type | required | description |
|------|------|----------|-------------|
| pendingDeploy | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |
| requestId | string | optional |  |
| activeDeploy | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |


## <a name="model-SingularityRequestHistory"></a> SingularityRequestHistory

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| message | string | optional |  |
| request | [SingularityRequest](#model-SingularityRequest) | optional |  |
| eventType | [RequestHistoryType](#model-RequestHistoryType) | optional |  Allowable values: CREATED, UPDATED, DELETED, PAUSED, UNPAUSED, ENTERED_COOLDOWN, EXITED_COOLDOWN, FINISHED, DEPLOYED_TO_UNPAUSE, BOUNCED, SCALED, SCALE_REVERTED |
| createdAt | long | optional |  |


## <a name="model-SingularityRequestParent"></a> SingularityRequestParent

| name | type | required | description |
|------|------|----------|-------------|
| expiringSkipHealthchecks | [SingularityExpiringSkipHealthchecks](#model-SingularityExpiringSkipHealthchecks) | optional |  |
| state | [RequestState](#model-RequestState) | optional |  Allowable values: ACTIVE, DELETED, PAUSED, SYSTEM_COOLDOWN, FINISHED, DEPLOYING_TO_UNPAUSE |
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
| runId | string | optional | An id to associate with this request which will be associated with the corresponding launched tasks |
| skipHealthchecks | boolean | optional | If set to true, healthchecks will be skipped for this task run |
| commandLineArgs | Array[string] | optional | Command line arguments to be passed to the task |
| message | string | optional | A message to show to users about why this action was taken |


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
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |
| instances | int | optional | The number of instances to scale to |


## <a name="model-SingularityShellCommand"></a> SingularityShellCommand

| name | type | required | description |
|------|------|----------|-------------|
| logfileName | string | optional |  |
| user | string | optional |  |
| options | Array[string] | optional |  |
| name | string | optional |  |


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
| activeSlaves | int | optional |  |
| generatedAt | long | optional |  |
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
| unknownSlaves | int | optional |  |
| activeRequests | int | optional |  |
| futureTasks | int | optional |  |
| lbCleanupRequests | int | optional |  |
| decommissioningRacks | int | optional |  |
| finishedRequests | int | optional |  |
| deadRacks | int | optional |  |
| pendingRequests | int | optional |  |
| maxTaskLag | long | optional |  |
| cooldownRequests | int | optional |  |
| hostStates | [Array[SingularityHostState]](#model-SingularityHostState) | optional |  |
| allRequests | int | optional |  |
| underProvisionedRequests | int | optional |  |
| decomissioningSlaves | int | optional |  |
| oldestDeploy | long | optional |  |
| scheduledTasks | int | optional |  |
| underProvisionedRequestIds | Array[string] | optional |  |


## <a name="model-SingularityTask"></a> SingularityTask

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| taskRequest | [SingularityTaskRequest](#model-SingularityTaskRequest) | optional |  |
| offer | [Offer](#model-Offer) | optional |  |
| mesosTask | [TaskInfo](#model-TaskInfo) | optional |  |
| rackId | string | optional |  |


## <a name="model-SingularityTaskCleanup"></a> SingularityTaskCleanup

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| user | string | optional |  |
| cleanupType | [TaskCleanupType](#model-TaskCleanupType) | optional |  Allowable values: USER_REQUESTED, USER_REQUESTED_TASK_BOUNCE, DECOMISSIONING, SCALING_DOWN, BOUNCING, INCREMENTAL_BOUNCE, DEPLOY_FAILED, NEW_DEPLOY_SUCCEEDED, DEPLOY_STEP_FINISHED, DEPLOY_CANCELED, UNHEALTHY_NEW_TASK, OVERDUE_NEW_TASK |
| message | string | optional |  |
| timestamp | long | optional |  |
| actionId | string | optional |  |


## <a name="model-SingularityTaskHealthcheckResult"></a> SingularityTaskHealthcheckResult

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
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
| shellCommandHistory | [Array[SingularityTaskShellCommandHistory]](#model-SingularityTaskShellCommandHistory) | optional |  |
| taskUpdates | [Array[SingularityTaskHistoryUpdate]](#model-SingularityTaskHistoryUpdate) | optional |  |


## <a name="model-SingularityTaskHistoryUpdate"></a> SingularityTaskHistoryUpdate

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| statusReason | string | optional |  |
| statusMessage | string | optional |  |
| taskState | [ExtendedTaskState](#model-ExtendedTaskState) | optional |  Allowable values: TASK_LAUNCHED, TASK_STAGING, TASK_STARTING, TASK_RUNNING, TASK_CLEANING, TASK_KILLING, TASK_FINISHED, TASK_FAILED, TASK_KILLED, TASK_LOST, TASK_LOST_WHILE_DOWN, TASK_ERROR |
| timestamp | long | optional |  |


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
| level | [MetadataLevel](#model-MetadataLevel) | optional |  |
| type | string | optional |  |
| message | string | optional |  |
| title | string | optional |  |


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
| id | string | optional |  |


## <a name="model-SingularityTaskShellCommandUpdate"></a> SingularityTaskShellCommandUpdate

| name | type | required | description |
|------|------|----------|-------------|
| updateType | [UpdateType](#model-UpdateType) | optional |  Allowable values: INVALID, ACKED, STARTED, FINISHED, FAILED |
| outputFilename | string | optional |  |
| message | string | optional |  |
| timestamp | long | optional |  |
| shellRequestId | [SingularityTaskShellCommandRequestId](#model-SingularityTaskShellCommandRequestId) | optional |  |


## <a name="model-SingularityUnpauseRequest"></a> SingularityUnpauseRequest

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | If set to true, instructs new tasks that are scheduled immediately while unpausing to skip healthchecks |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |


## <a name="model-SingularityUpdatePendingDeployRequest"></a> SingularityUpdatePendingDeployRequest

| name | type | required | description |
|------|------|----------|-------------|
| requestId | string | optional |  |
| deployId | string | optional |  |
| targetActiveInstances | int | optional |  |


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


## <a name="model-SlaveID"></a> SlaveID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [SlaveID](#model-SlaveID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$SlaveID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$SlaveID&gt;) | optional |  |
| initialized | boolean | optional |  |
| value | string | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-SlaveIDOrBuilder"></a> SlaveIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-TaskID"></a> TaskID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [TaskID](#model-TaskID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskID&gt;) | optional |  |
| initialized | boolean | optional |  |
| value | string | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-TaskIDOrBuilder"></a> TaskIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-TaskInfo"></a> TaskInfo

| name | type | required | description |
|------|------|----------|-------------|
| commandOrBuilder | [CommandInfoOrBuilder](#model-CommandInfoOrBuilder) | optional |  |
| defaultInstanceForType | [TaskInfo](#model-TaskInfo) | optional |  |
| taskIdOrBuilder | [TaskIDOrBuilder](#model-TaskIDOrBuilder) | optional |  |
| taskId | [TaskID](#model-TaskID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskInfo&gt;) | optional |  |
| slaveIdOrBuilder | [SlaveIDOrBuilder](#model-SlaveIDOrBuilder) | optional |  |
| resourcesOrBuilderList | [List[? extends org.apache.mesos.Protos$ResourceOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ResourceOrBuilder]) | optional |  |
| labelsOrBuilder | [LabelsOrBuilder](#model-LabelsOrBuilder) | optional |  |
| data | [ByteString](#model-ByteString) | optional |  |
| executor | [ExecutorInfo](#model-ExecutorInfo) | optional |  |
| containerOrBuilder | [ContainerInfoOrBuilder](#model-ContainerInfoOrBuilder) | optional |  |
| labels | [Labels](#model-Labels) | optional |  |
| executorOrBuilder | [ExecutorInfoOrBuilder](#model-ExecutorInfoOrBuilder) | optional |  |
| container | [ContainerInfo](#model-ContainerInfo) | optional |  |
| healthCheckOrBuilder | [HealthCheckOrBuilder](#model-HealthCheckOrBuilder) | optional |  |
| initialized | boolean | optional |  |
| name | string | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| command | [CommandInfo](#model-CommandInfo) | optional |  |
| healthCheck | [HealthCheck](#model-HealthCheck) | optional |  |
| serializedSize | int | optional |  |
| resourcesList | [List[Resource]](#model-List[Resource]) | optional |  |
| slaveId | [SlaveID](#model-SlaveID) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| discovery | [DiscoveryInfo](#model-DiscoveryInfo) | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| resourcesCount | int | optional |  |
| initializationErrorString | string | optional |  |
| discoveryOrBuilder | [DiscoveryInfoOrBuilder](#model-DiscoveryInfoOrBuilder) | optional |  |


## <a name="model-TimeInfo"></a> TimeInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [TimeInfo](#model-TimeInfo) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TimeInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TimeInfo&gt;) | optional |  |
| nanoseconds | long | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| initializationErrorString | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-TimeInfoOrBuilder"></a> TimeInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| nanoseconds | long | optional |  |


## <a name="model-URL"></a> URL

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [URL](#model-URL) | optional |  |
| queryCount | int | optional |  |
| queryOrBuilderList | [List[? extends org.apache.mesos.Protos$ParameterOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ParameterOrBuilder]) | optional |  |
| queryList | [List[Parameter]](#model-List[Parameter]) | optional |  |
| fragment | string | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$URL&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$URL&gt;) | optional |  |
| address | [Address](#model-Address) | optional |  |
| schemeBytes | [ByteString](#model-ByteString) | optional |  |
| addressOrBuilder | [AddressOrBuilder](#model-AddressOrBuilder) | optional |  |
| pathBytes | [ByteString](#model-ByteString) | optional |  |
| scheme | string | optional |  |
| fragmentBytes | [ByteString](#model-ByteString) | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| path | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-URLOrBuilder"></a> URLOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| queryCount | int | optional |  |
| queryOrBuilderList | [List[? extends org.apache.mesos.Protos$ParameterOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ParameterOrBuilder]) | optional |  |
| queryList | [List[Parameter]](#model-List[Parameter]) | optional |  |
| fragment | string | optional |  |
| address | [Address](#model-Address) | optional |  |
| schemeBytes | [ByteString](#model-ByteString) | optional |  |
| addressOrBuilder | [AddressOrBuilder](#model-AddressOrBuilder) | optional |  |
| pathBytes | [ByteString](#model-ByteString) | optional |  |
| fragmentBytes | [ByteString](#model-ByteString) | optional |  |
| scheme | string | optional |  |
| path | string | optional |  |


## <a name="model-Unavailability"></a> Unavailability

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [Unavailability](#model-Unavailability) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Unavailability&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Unavailability&gt;) | optional |  |
| durationOrBuilder | [DurationInfoOrBuilder](#model-DurationInfoOrBuilder) | optional |  |
| initialized | boolean | optional |  |
| startOrBuilder | [TimeInfoOrBuilder](#model-TimeInfoOrBuilder) | optional |  |
| serializedSize | int | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| duration | [DurationInfo](#model-DurationInfo) | optional |  |
| start | [TimeInfo](#model-TimeInfo) | optional |  |
| initializationErrorString | string | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |


## <a name="model-UnavailabilityOrBuilder"></a> UnavailabilityOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| durationOrBuilder | [DurationInfoOrBuilder](#model-DurationInfoOrBuilder) | optional |  |
| startOrBuilder | [TimeInfoOrBuilder](#model-TimeInfoOrBuilder) | optional |  |
| duration | [DurationInfo](#model-DurationInfo) | optional |  |
| start | [TimeInfo](#model-TimeInfo) | optional |  |


## <a name="model-UnknownFieldSet"></a> UnknownFieldSet

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| serializedSizeAsMessageSet | int | optional |  |
| parserForType | [Parser](#model-Parser) | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |


