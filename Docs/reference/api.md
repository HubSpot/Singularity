# Singularity REST API

Version: 0.4.2-SNAPSHOT

Endpoints:
- [`/api/deploys`](#endpoint-0) - Manages Singularity Deploys for existing requests
- [`/api/history`](#endpoint-1) - Manages historical data for tasks, requests, and deploys.
- [`/api/logs`](#endpoint-2) - Manages Singularity task logs stored in S3.
- [`/api/racks`](#endpoint-3) - Manages Singularity racks.
- [`/api/requests`](#endpoint-4) - Manages Singularity Requests, the parent object for any deployed task
- [`/api/sandbox`](#endpoint-5) - Provides a proxy to Mesos sandboxes.
- [`/api/slaves`](#endpoint-6) - Manages Singularity slaves.
- [`/api/state`](#endpoint-7) - Provides information about the current state of Singularity.
- [`/api/tasks`](#endpoint-8) - Manages Singularity tasks.
- [`/api/test`](#endpoint-9) - Misc testing endpoints.
- [`/api/webhooks`](#endpoint-10) - Manages Singularity webhooks.

Models:
- [`ByteString`](#model-ByteString)
- [`CommandInfo`](#model-CommandInfo)
- [`CommandInfoOrBuilder`](#model-CommandInfoOrBuilder)
- [`ContainerInfo`](#model-ContainerInfo)
- [`ContainerInfoOrBuilder`](#model-ContainerInfoOrBuilder)
- [`Descriptor`](#model-Descriptor)
- [`DockerInfo`](#model-DockerInfo)
- [`DockerInfoOrBuilder`](#model-DockerInfoOrBuilder)
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
- [`LoadBalancerRequestId`](#model-LoadBalancerRequestId)
- [`MesosFileChunkObject`](#model-MesosFileChunkObject)
- [`MesosTaskStatisticsObject`](#model-MesosTaskStatisticsObject)
- [`MessageOptions`](#model-MessageOptions)
- [`Offer`](#model-Offer)
- [`OfferID`](#model-OfferID)
- [`OfferIDOrBuilder`](#model-OfferIDOrBuilder)
- [`S3Artifact`](#model-S3Artifact)
- [`SingularityContainerInfo`](#model-SingularityContainerInfo)
- [`SingularityDeploy`](#model-SingularityDeploy)
- [`SingularityDeployHistory`](#model-SingularityDeployHistory)
- [`SingularityDeployMarker`](#model-SingularityDeployMarker)
- [`SingularityDeployRequest`](#model-SingularityDeployRequest)
- [`SingularityDeployResult`](#model-SingularityDeployResult)
- [`SingularityDeployStatistics`](#model-SingularityDeployStatistics)
- [`SingularityDeployWebhook`](#model-SingularityDeployWebhook)
- [`SingularityDockerInfo`](#model-SingularityDockerInfo)
- [`SingularityDockerPortMapping`](#model-SingularityDockerPortMapping)
- [`SingularityHostState`](#model-SingularityHostState)
- [`SingularityLoadBalancerUpdate`](#model-SingularityLoadBalancerUpdate)
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
- [`SingularityRequestInstances`](#model-SingularityRequestInstances)
- [`SingularityRequestParent`](#model-SingularityRequestParent)
- [`SingularitySandbox`](#model-SingularitySandbox)
- [`SingularitySandboxFile`](#model-SingularitySandboxFile)
- [`SingularitySlave`](#model-SingularitySlave)
- [`SingularityState`](#model-SingularityState)
- [`SingularityTask`](#model-SingularityTask)
- [`SingularityTaskCleanup`](#model-SingularityTaskCleanup)
- [`SingularityTaskCleanupResult`](#model-SingularityTaskCleanupResult)
- [`SingularityTaskHealthcheckResult`](#model-SingularityTaskHealthcheckResult)
- [`SingularityTaskHistory`](#model-SingularityTaskHistory)
- [`SingularityTaskHistoryUpdate`](#model-SingularityTaskHistoryUpdate)
- [`SingularityTaskId`](#model-SingularityTaskId)
- [`SingularityTaskIdHistory`](#model-SingularityTaskIdHistory)
- [`SingularityTaskRequest`](#model-SingularityTaskRequest)
- [`SingularityVolume`](#model-SingularityVolume)
- [`SingularityWebhook`](#model-SingularityWebhook)
- [`SlaveID`](#model-SlaveID)
- [`SlaveIDOrBuilder`](#model-SlaveIDOrBuilder)
- [`TaskID`](#model-TaskID)
- [`TaskIDOrBuilder`](#model-TaskIDOrBuilder)
- [`TaskInfo`](#model-TaskInfo)
- [`UnknownFieldSet`](#model-UnknownFieldSet)

- - -

## Endpoints
### <a name="#endpoint-0"></a> /api/deploys
#### Overview
Manages Singularity Deploys for existing requests

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
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | The user which executes the delete request. | string |

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
### <a name="#endpoint-1"></a> /api/history
#### Overview
Manages historical data for tasks, requests, and deploys.

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
List[string]


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

Retrieve the history for all tasks of a specific request.


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
[List[SingularityTaskIdHistory]](#model-SingularityTaskIdHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/requests`




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
### <a name="#endpoint-2"></a> /api/logs
#### Overview
Manages Singularity task logs stored in S3.

#### **GET** `/api/logs/task/{taskId}`

Retrieve the list of logs stored in S3 for a specific task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true | The task ID to search for | string |

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

###### Response
[List[SingularityS3Log]](#model-SingularityS3Log)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="#endpoint-3"></a> /api/racks
#### Overview
Manages Singularity racks.

#### **DELETE** `/api/racks/rack/{rackId}/decomissioning`

Undo the decomission operation on a specific decommissioning rack.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Decommissioned rack ID. | string |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/racks/rack/{rackId}/decomission`

Decomission a specific active rack.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Active rack ID. | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of person requestin the decommisioning. | string |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/racks/rack/{rackId}/dead`

Remove a dead rack.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Dead rack ID. | string |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/racks/decomissioning`

Retrieve the list of decommissioning racks.


###### Parameters
- No parameters

###### Response
[List[SingularityRack]](#model-SingularityRack)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/racks/dead`

Retrieve the list of dead racks. A rack is dead if it has zero active slaves.


###### Parameters
- No parameters

###### Response
[List[SingularityRack]](#model-SingularityRack)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/racks/active`

Retrieve the list of active racks. A rack is active if it has one or more active slaves associated with it.


###### Parameters
- No parameters

###### Response
[List[SingularityRack]](#model-SingularityRack)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="#endpoint-4"></a> /api/requests
#### Overview
Manages Singularity Requests, the parent object for any deployed task

#### **POST** `/api/requests/request/{requestId}/unpause`

Unpause a Singularity Request, scheduling new tasks immediately


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to unpause | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of the person requesting the unpause | string |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 409    | Request is not paused | - |


- - -
#### **POST** `/api/requests/request/{requestId}/run`

Schedule a one-off or scheduled Singularity request for immediate execution.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to run | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of the person requesting the execution | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Additional command line arguments to append to the task | string |

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
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of the person requesting the pause | string |
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
#### **PUT** `/api/requests/request/{requestId}/instances`

Scale the number of instances up or down for a specific Request


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID to scale | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of the person requesting the scale | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Object to hold number of instances to request | [SingularityRequestInstances](#model-linkType)</a> |

###### Response
[SingularityRequest](#model-SingularityRequest)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Posted object did not match Request ID | - |
| 404    | No Request with that ID | - |


- - -
#### **POST** `/api/requests/request/{requestId}/bounce`

Bounce a specific Singularity request. A bounce launches replacement task(s), and then kills the original task(s) if the replacement(s) are healthy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to bounce | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of the person requesting the bounce | string |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


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
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of the person requesting the delete | string |

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
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of the person requesting to create or update | string |
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
### <a name="#endpoint-5"></a> /api/sandbox
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
### <a name="#endpoint-6"></a> /api/slaves
#### Overview
Manages Singularity slaves.

#### **DELETE** `/api/slaves/slave/{slaveId}/decomissioning`

Remove a specific decommissioning slave


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| slaveId | true |  | string |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/slaves/slave/{slaveId}/decomission`

Decommission a specific slave.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| slaveId | true |  | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false |  | string |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/slaves/slave/{slaveId}/dead`

Remove a specific dead slave.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| slaveId | true |  | string |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/slaves/decomissioning`

Retrieve the list of decommissioning slaves.


###### Parameters
- No parameters

###### Response
[List[SingularitySlave]](#model-SingularitySlave)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/slaves/dead`

Retrieve the list of dead slaves.


###### Parameters
- No parameters

###### Response
[List[SingularitySlave]](#model-SingularitySlave)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/slaves/active`

Retrieve the list of active slaves.


###### Parameters
- No parameters

###### Response
[List[SingularitySlave]](#model-SingularitySlave)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="#endpoint-7"></a> /api/state
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
### <a name="#endpoint-8"></a> /api/tasks
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

Kill a specific active task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false |  | string |

###### Response
[SingularityTaskCleanupResult](#model-SingularityTaskCleanupResult)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


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
[List[SingularityPendingTaskId]](#model-SingularityPendingTaskId)


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
[List[SingularityTaskId]](#model-SingularityTaskId)


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
[List[SingularityTaskCleanup]](#model-SingularityTaskCleanup)


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
[List[SingularityTask]](#model-SingularityTask)


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
[List[SingularityTask]](#model-SingularityTask)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### <a name="#endpoint-9"></a> /api/test
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
### <a name="#endpoint-10"></a> /api/webhooks
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
#### **GET** `/api/webhooks/deploy/{webhookId}`

Retrieve a list of queued deploy updates for a specific webhook.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | true |  | string |

###### Response
[List[SingularityDeployWebhook]](#model-SingularityDeployWebhook)


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

## Data Types

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
| containerOrBuilder | [ContainerInfoOrBuilder](#model-ContainerInfoOrBuilder) | optional |  |
| container | [ContainerInfo](#model-ContainerInfo) | optional |  |
| user | string | optional |  |
| initialized | boolean | optional |  |
| value | string | optional |  |
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
| urisCount | int | optional |  |
| argumentsCount | int | optional |  |
| argumentsList | Array[string] | optional |  |
| containerOrBuilder | [ContainerInfoOrBuilder](#model-ContainerInfoOrBuilder) | optional |  |
| container | [ContainerInfo](#model-ContainerInfo) | optional |  |
| user | string | optional |  |
| value | string | optional |  |
| environment | [Environment](#model-Environment) | optional |  |
| userBytes | [ByteString](#model-ByteString) | optional |  |
| shell | boolean | optional |  |
| urisList | [List[URI]](#model-List[URI]) | optional |  |
| environmentOrBuilder | [EnvironmentOrBuilder](#model-EnvironmentOrBuilder) | optional |  |
| valueBytes | [ByteString](#model-ByteString) | optional |  |


## <a name="model-ContainerInfo"></a> ContainerInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [ContainerInfo](#model-ContainerInfo) | optional |  |
| type | [Type](#model-Type) | optional |  Allowable values: DOCKER, MESOS |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo&gt;) | optional |  |
| hostname | string | optional |  |
| dockerOrBuilder | [DockerInfoOrBuilder](#model-DockerInfoOrBuilder) | optional |  |
| initialized | boolean | optional |  |
| volumesCount | int | optional |  |
| serializedSize | int | optional |  |
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
| type | [Type](#model-Type) | optional |  Allowable values: DOCKER, MESOS |
| hostname | string | optional |  |
| dockerOrBuilder | [DockerInfoOrBuilder](#model-DockerInfoOrBuilder) | optional |  |
| volumesCount | int | optional |  |
| volumesList | [List[Volume]](#model-List[Volume]) | optional |  |
| hostnameBytes | [ByteString](#model-ByteString) | optional |  |
| docker | [DockerInfo](#model-DockerInfo) | optional |  |
| volumesOrBuilderList | [List[? extends org.apache.mesos.Protos$VolumeOrBuilder]](#model-List[? extends org.apache.mesos.Protos$VolumeOrBuilder]) | optional |  |


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


## <a name="model-DockerInfo"></a> DockerInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [DockerInfo](#model-DockerInfo) | optional |  |
| portMappingsOrBuilderList | [List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]) | optional |  |
| parametersList | [List[Parameter]](#model-List[Parameter]) | optional |  |
| parametersOrBuilderList | [List[? extends org.apache.mesos.Protos$ParameterOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ParameterOrBuilder]) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo$DockerInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo$DockerInfo&gt;) | optional |  |
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
| parametersOrBuilderList | [List[? extends org.apache.mesos.Protos$ParameterOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ParameterOrBuilder]) | optional |  |
| parametersList | [List[Parameter]](#model-List[Parameter]) | optional |  |
| imageBytes | [ByteString](#model-ByteString) | optional |  |
| privileged | boolean | optional |  |
| parametersCount | int | optional |  |
| portMappingsCount | int | optional |  |
| network | [Network](#model-Network) | optional |  Allowable values: HOST, BRIDGE, NONE |
| portMappingsList | [List[PortMapping]](#model-List[PortMapping]) | optional |  |
| image | string | optional |  |


## <a name="model-EmbeddedArtifact"></a> EmbeddedArtifact

| name | type | required | description |
|------|------|----------|-------------|
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
| loggingExtraFields | [Map[string,string]](#model-Map[string,string]) | optional |  |
| successfulExitCodes | Array[int] | optional |  |
| s3Artifacts | [Array[S3Artifact]](#model-S3Artifact) | optional |  |
| embeddedArtifacts | [Array[EmbeddedArtifact]](#model-EmbeddedArtifact) | optional |  |
| runningSentinel | string | optional |  |
| externalArtifacts | [Array[ExternalArtifact]](#model-ExternalArtifact) | optional |  |
| user | string | optional |  |
| extraCmdLineArgs | Array[string] | optional |  |
| loggingTag | string | optional |  |
| sigKillProcessesAfterMillis | long | optional |  |
| maxTaskThreads | int | optional |  |
| cmd | string | optional |  |


## <a name="model-ExecutorID"></a> ExecutorID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [ExecutorID](#model-ExecutorID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorID&gt;) | optional |  |
| value | string | optional |  |
| initialized | boolean | optional |  |
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
| container | [ContainerInfo](#model-ContainerInfo) | optional |  |
| executorId | [ExecutorID](#model-ExecutorID) | optional |  |
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
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| resourcesCount | int | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-ExecutorInfoOrBuilder"></a> ExecutorInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| commandOrBuilder | [CommandInfoOrBuilder](#model-CommandInfoOrBuilder) | optional |  |
| resourcesOrBuilderList | [List[? extends org.apache.mesos.Protos$ResourceOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ResourceOrBuilder]) | optional |  |
| data | [ByteString](#model-ByteString) | optional |  |
| source | string | optional |  |
| containerOrBuilder | [ContainerInfoOrBuilder](#model-ContainerInfoOrBuilder) | optional |  |
| container | [ContainerInfo](#model-ContainerInfo) | optional |  |
| executorId | [ExecutorID](#model-ExecutorID) | optional |  |
| name | string | optional |  |
| nameBytes | [ByteString](#model-ByteString) | optional |  |
| sourceBytes | [ByteString](#model-ByteString) | optional |  |
| command | [CommandInfo](#model-CommandInfo) | optional |  |
| frameworkId | [FrameworkID](#model-FrameworkID) | optional |  |
| frameworkIdOrBuilder | [FrameworkIDOrBuilder](#model-FrameworkIDOrBuilder) | optional |  |
| executorIdOrBuilder | [ExecutorIDOrBuilder](#model-ExecutorIDOrBuilder) | optional |  |
| resourcesList | [List[Resource]](#model-List[Resource]) | optional |  |
| resourcesCount | int | optional |  |


## <a name="model-ExternalArtifact"></a> ExternalArtifact

| name | type | required | description |
|------|------|----------|-------------|
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
| javaMultipleFiles | boolean | optional |  |
| defaultInstanceForType | [FileOptions](#model-FileOptions) | optional |  |
| optimizeFor | [OptimizeMode](#model-OptimizeMode) | optional |  Allowable values: SPEED, CODE_SIZE, LITE_RUNTIME |
| javaPackageBytes | [ByteString](#model-ByteString) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$FileOptions&gt;](#model-com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$FileOptions&gt;) | optional |  |
| goPackageBytes | [ByteString](#model-ByteString) | optional |  |
| uninterpretedOptionCount | int | optional |  |
| javaGenericServices | boolean | optional |  |
| javaOuterClassnameBytes | [ByteString](#model-ByteString) | optional |  |
| initialized | boolean | optional |  |
| javaOuterClassname | string | optional |  |
| serializedSize | int | optional |  |
| pyGenericServices | boolean | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| javaPackage | string | optional |  |
| uninterpretedOptionList | [List[UninterpretedOption]](#model-List[UninterpretedOption]) | optional |  |
| goPackage | string | optional |  |
| javaGenerateEqualsAndHash | boolean | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| uninterpretedOptionOrBuilderList | [List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]](#model-List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]) | optional |  |
| initializationErrorString | string | optional |  |
| ccGenericServices | boolean | optional |  |


## <a name="model-FrameworkID"></a> FrameworkID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [FrameworkID](#model-FrameworkID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$FrameworkID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$FrameworkID&gt;) | optional |  |
| value | string | optional |  |
| initialized | boolean | optional |  |
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


## <a name="model-LoadBalancerRequestId"></a> LoadBalancerRequestId

| name | type | required | description |
|------|------|----------|-------------|
| requestType | [LoadBalancerRequestType](#model-LoadBalancerRequestType) | optional |  Allowable values: ADD, REMOVE, DEPLOY |
| attemptNumber | int | optional |  |
| id | string | optional |  |


## <a name="model-MesosFileChunkObject"></a> MesosFileChunkObject

| name | type | required | description |
|------|------|----------|-------------|
| data | string | optional |  |
| offset | long | optional |  |


## <a name="model-MesosTaskStatisticsObject"></a> MesosTaskStatisticsObject

| name | type | required | description |
|------|------|----------|-------------|
| memFileBytes | long | optional |  |
| memLimitBytes | long | optional |  |
| cpusThrottledTimeSecs | float | optional |  |
| cpusSystemTimeSecs | float | optional |  |
| memRssBytes | long | optional |  |
| memAnonBytes | long | optional |  |
| memMappedFileBytes | long | optional |  |
| cpusLimit | int | optional |  |
| timestamp | double | optional |  |
| cpusNrPeriods | int | optional |  |
| cpusUserTimeSecs | float | optional |  |
| cpusNrThrottled | int | optional |  |


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
| executorIdsCount | int | optional |  |
| resourcesOrBuilderList | [List[? extends org.apache.mesos.Protos$ResourceOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ResourceOrBuilder]) | optional |  |
| executorIdsList | [List[ExecutorID]](#model-List[ExecutorID]) | optional |  |
| hostname | string | optional |  |
| attributesCount | int | optional |  |
| initialized | boolean | optional |  |
| idOrBuilder | [OfferIDOrBuilder](#model-OfferIDOrBuilder) | optional |  |
| attributesList | [List[Attribute]](#model-List[Attribute]) | optional |  |
| frameworkId | [FrameworkID](#model-FrameworkID) | optional |  |
| frameworkIdOrBuilder | [FrameworkIDOrBuilder](#model-FrameworkIDOrBuilder) | optional |  |
| serializedSize | int | optional |  |
| resourcesList | [List[Resource]](#model-List[Resource]) | optional |  |
| slaveId | [SlaveID](#model-SlaveID) | optional |  |
| allFields | [Map[FieldDescriptor,Object]](#model-Map[FieldDescriptor,Object]) | optional |  |
| hostnameBytes | [ByteString](#model-ByteString) | optional |  |
| descriptorForType | [Descriptor](#model-Descriptor) | optional |  |
| attributesOrBuilderList | [List[? extends org.apache.mesos.Protos$AttributeOrBuilder]](#model-List[? extends org.apache.mesos.Protos$AttributeOrBuilder]) | optional |  |
| resourcesCount | int | optional |  |
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| id | [OfferID](#model-OfferID) | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-OfferID"></a> OfferID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [OfferID](#model-OfferID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$OfferID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$OfferID&gt;) | optional |  |
| value | string | optional |  |
| initialized | boolean | optional |  |
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


## <a name="model-S3Artifact"></a> S3Artifact

| name | type | required | description |
|------|------|----------|-------------|
| s3Bucket | string | optional |  |
| md5sum | string | optional |  |
| filename | string | optional |  |
| filesize | long | optional |  |
| s3ObjectKey | string | optional |  |
| name | string | optional |  |


## <a name="model-SingularityContainerInfo"></a> SingularityContainerInfo

| name | type | required | description |
|------|------|----------|-------------|
| type | [Type](#model-Type) | optional |  Allowable values: DOCKER, MESOS |
| volumes | [Array[SingularityVolume]](#model-SingularityVolume) | optional |  |
| docker | [SingularityDockerInfo](#model-SingularityDockerInfo) | optional |  |


## <a name="model-SingularityDeploy"></a> SingularityDeploy

| name | type | required | description |
|------|------|----------|-------------|
| customExecutorId | string | optional | Custom Mesos executor id. |
| resources | [com.hubspot.mesos.Resources](#model-com.hubspot.mesos.Resources) | optional | Resources required for this deploy. |
| uris | Array[string] | optional | List of URIs to download before executing the deploy command. |
| containerInfo | [SingularityContainerInfo](#model-SingularityContainerInfo) | optional | Container information for deployment into a container. |
| arguments | Array[string] | optional | Command arguments. |
| serviceBasePath | string | optional | The base path for the API exposed by the deploy. Used in conjunction with the Load balancer API. |
| metadata | [Map[string,string]](#model-Map[string,string]) | optional | Map of metadata key/value pairs associated with the deployment. |
| customExecutorSource | string | optional | Custom Mesos executor source. |
| healthcheckTimeoutSeconds | long | optional | Health check timeout in seconds. |
| healthcheckUri | string | optional | Deployment Healthcheck URI. |
| requestId | string | required | Singularity Request Id which is associated with this deploy. |
| loadBalancerGroups | Array[string] | optional | List of load balancer groups associated with this deployment. |
| skipHealthchecksOnDeploy | boolean | optional | Allows skipping of health checks when deploying. |
| healthcheckIntervalSeconds | long | optional | Health check interval in seconds. |
| executorData | [ExecutorData](#model-ExecutorData) | optional | Executor specific information |
| command | string | optional | Command to execute for this deployment. |
| considerHealthyAfterRunningForSeconds | long | optional | Number of seconds that a service must be healthy to consider the deployment to be successful. |
| timestamp | long | optional | Deploy timestamp. |
| loadBalancerOptions | [Map[string,Object]](#model-Map[string,Object]) | optional | Map (Key/Value) of options for the load balancer. |
| customExecutorCmd | string | optional | Custom Mesos executor |
| env | [Map[string,string]](#model-Map[string,string]) | optional | Map of environment variable definitions. |
| version | string | optional | Deploy version |
| deployHealthTimeoutSeconds | long | optional | Number of seconds that singularity waits for this service to become healthy. |
| id | string | required | Singularity deploy id. |


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
| timestamp | long | optional |  |
| deployId | string | optional |  |


## <a name="model-SingularityDeployRequest"></a> SingularityDeployRequest

| name | type | required | description |
|------|------|----------|-------------|
| unpauseOnSuccessfulDeploy | boolean | optional | If deploy is successful, also unpause the request. |
| deploy | [SingularityDeploy](#model-SingularityDeploy) | required | The Singularity deploy object |
| user | string | optional | User owning this deploy. |


## <a name="model-SingularityDeployResult"></a> SingularityDeployResult

| name | type | required | description |
|------|------|----------|-------------|
| lbUpdate | [SingularityLoadBalancerUpdate](#model-SingularityLoadBalancerUpdate) | optional |  |
| deployState | [DeployState](#model-DeployState) | optional |  Allowable values: SUCCEEDED, FAILED_INTERNAL_STATE, CANCELING, WAITING, OVERDUE, FAILED, CANCELED |
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


## <a name="model-SingularityDeployWebhook"></a> SingularityDeployWebhook

| name | type | required | description |
|------|------|----------|-------------|
| deploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| deployResult | [SingularityDeployResult](#model-SingularityDeployResult) | optional |  |
| eventType | [DeployEventType](#model-DeployEventType) | optional |  Allowable values: STARTING, FINISHED |
| deployMarker | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |


## <a name="model-SingularityDockerInfo"></a> SingularityDockerInfo

| name | type | required | description |
|------|------|----------|-------------|
| privileged | boolean | optional |  |
| network | [Network](#model-Network) | optional |  |
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


## <a name="model-SingularityHostState"></a> SingularityHostState

| name | type | required | description |
|------|------|----------|-------------|
| hostAddress | string | optional |  |
| hostname | string | optional |  |
| driverStatus | string | optional |  |
| master | boolean | optional |  |
| mesosMaster | string | optional |  |
| uptime | long | optional |  |
| millisSinceLastOffer | long | optional |  |


## <a name="model-SingularityLoadBalancerUpdate"></a> SingularityLoadBalancerUpdate

| name | type | required | description |
|------|------|----------|-------------|
| loadBalancerState | [BaragonRequestState](#model-BaragonRequestState) | optional |  Allowable values: UNKNOWN, FAILED, WAITING, SUCCESS, CANCELING, CANCELED |
| loadBalancerRequestId | [LoadBalancerRequestId](#model-LoadBalancerRequestId) | optional |  |
| uri | string | optional |  |
| method | [LoadBalancerMethod](#model-LoadBalancerMethod) | optional |  Allowable values: PRE_ENQUEUE, ENQUEUE, CHECK_STATE, CANCEL |
| message | string | optional |  |
| timestamp | long | optional |  |


## <a name="model-SingularityPauseRequest"></a> SingularityPauseRequest

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| killTasks | boolean | optional |  |


## <a name="model-SingularityPendingDeploy"></a> SingularityPendingDeploy

| name | type | required | description |
|------|------|----------|-------------|
| currentDeployState | [DeployState](#model-DeployState) | optional |  Allowable values: SUCCEEDED, FAILED_INTERNAL_STATE, CANCELING, WAITING, OVERDUE, FAILED, CANCELED |
| lastLoadBalancerUpdate | [SingularityLoadBalancerUpdate](#model-SingularityLoadBalancerUpdate) | optional |  |
| deployMarker | [SingularityDeployMarker](#model-SingularityDeployMarker) | optional |  |


## <a name="model-SingularityPendingRequest"></a> SingularityPendingRequest

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| requestId | string | optional |  |
| cmdLineArgs | string | optional |  |
| timestamp | long | optional |  |
| deployId | string | optional |  |
| pendingType | [PendingType](#model-PendingType) | optional |  Allowable values: IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP |


## <a name="model-SingularityPendingTask"></a> SingularityPendingTask

| name | type | required | description |
|------|------|----------|-------------|
| pendingTaskId | [SingularityPendingTaskId](#model-SingularityPendingTaskId) | optional |  |
| maybeCmdLineArgs | string | optional |  |


## <a name="model-SingularityPendingTaskId"></a> SingularityPendingTaskId

| name | type | required | description |
|------|------|----------|-------------|
| nextRunAt | long | optional |  |
| requestId | string | optional |  |
| deployId | string | optional |  |
| pendingType | [PendingType](#model-PendingType) | optional |  Allowable values: IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP |
| instanceNo | int | optional |  |
| createdAt | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityRack"></a> SingularityRack

| name | type | required | description |
|------|------|----------|-------------|
| deadAt | long | optional |  |
| decomissionedAt | long | optional |  |
| state | [SingularityMachineState](#model-SingularityMachineState) | optional |  Allowable values: ACTIVE, DECOMISSIONING, DECOMISSIONED, DEAD |
| decomissioningBy | string | optional |  |
| decomissioningAt | long | optional |  |
| firstSeenAt | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityRequest"></a> SingularityRequest

| name | type | required | description |
|------|------|----------|-------------|
| schedule | string | optional |  |
| rackAffinity | Array[string] | optional |  |
| daemon | boolean | optional |  |
| slavePlacement | [SlavePlacement](#model-SlavePlacement) | optional |  |
| rackSensitive | boolean | optional |  |
| owners | Array[string] | optional |  |
| quartzSchedule | string | optional |  |
| scheduledExpectedRuntimeMillis | long | optional |  |
| loadBalanced | boolean | optional |  |
| numRetriesOnFailure | int | optional |  |
| killOldNonLongRunningTasksAfterMillis | long | optional |  |
| instances | int | optional |  |
| scheduleType | [ScheduleType](#model-ScheduleType) | optional |  |
| id | string | optional |  |


## <a name="model-SingularityRequestCleanup"></a> SingularityRequestCleanup

| name | type | required | description |
|------|------|----------|-------------|
| requestId | string | optional |  |
| user | string | optional |  |
| killTasks | boolean | optional |  |
| cleanupType | [RequestCleanupType](#model-RequestCleanupType) | optional |  Allowable values: DELETING, PAUSING, BOUNCE |
| timestamp | long | optional |  |
| deployId | string | optional |  |


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
| request | [SingularityRequest](#model-SingularityRequest) | optional |  |
| eventType | [RequestHistoryType](#model-RequestHistoryType) | optional |  Allowable values: CREATED, UPDATED, DELETED, PAUSED, UNPAUSED, ENTERED_COOLDOWN, EXITED_COOLDOWN, FINISHED, DEPLOYED_TO_UNPAUSE |
| createdAt | long | optional |  |


## <a name="model-SingularityRequestInstances"></a> SingularityRequestInstances

| name | type | required | description |
|------|------|----------|-------------|
| instances | int | optional |  |
| id | string | optional |  |


## <a name="model-SingularityRequestParent"></a> SingularityRequestParent

| name | type | required | description |
|------|------|----------|-------------|
| state | [RequestState](#model-RequestState) | optional |  Allowable values: ACTIVE, DELETED, PAUSED, SYSTEM_COOLDOWN, FINISHED, DEPLOYING_TO_UNPAUSE |
| pendingDeploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| activeDeploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| request | [SingularityRequest](#model-SingularityRequest) | optional |  |
| pendingDeployState | [SingularityPendingDeploy](#model-SingularityPendingDeploy) | optional |  |
| requestDeployState | [SingularityRequestDeployState](#model-SingularityRequestDeployState) | optional |  |


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


## <a name="model-SingularitySlave"></a> SingularitySlave

| name | type | required | description |
|------|------|----------|-------------|
| deadAt | long | optional |  |
| decomissionedAt | long | optional |  |
| state | [SingularityMachineState](#model-SingularityMachineState) | optional |  Allowable values: ACTIVE, DECOMISSIONING, DECOMISSIONED, DEAD |
| host | string | optional | Slave hostname |
| decomissioningBy | string | optional |  |
| decomissioningAt | long | optional |  |
| rackId | string | optional | Slave rack ID |
| firstSeenAt | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityState"></a> SingularityState

| name | type | required | description |
|------|------|----------|-------------|
| activeRacks | int | optional |  |
| decomissioningRacks | int | optional |  |
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
| numDeploys | int | optional |  |
| cleaningTasks | int | optional |  |
| activeRequests | int | optional |  |
| futureTasks | int | optional |  |
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


## <a name="model-SingularityTaskCleanup"></a> SingularityTaskCleanup

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| user | string | optional |  |
| cleanupType | [TaskCleanupType](#model-TaskCleanupType) | optional |  Allowable values: USER_REQUESTED, DECOMISSIONING, SCALING_DOWN, BOUNCING, DEPLOY_FAILED, NEW_DEPLOY_SUCCEEDED, DEPLOY_CANCELED, UNHEALTHY_NEW_TASK, OVERDUE_NEW_TASK |
| timestamp | long | optional |  |


## <a name="model-SingularityTaskCleanupResult"></a> SingularityTaskCleanupResult

| name | type | required | description |
|------|------|----------|-------------|
| result | [SingularityCreateResult](#model-SingularityCreateResult) | optional |  Allowable values: CREATED, EXISTED |
| task | [SingularityTask](#model-SingularityTask) | optional |  |


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
| taskUpdates | [Array[SingularityTaskHistoryUpdate]](#model-SingularityTaskHistoryUpdate) | optional |  |


## <a name="model-SingularityTaskHistoryUpdate"></a> SingularityTaskHistoryUpdate

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| statusMessage | string | optional |  |
| taskState | [ExtendedTaskState](#model-ExtendedTaskState) | optional |  Allowable values: TASK_LAUNCHED, TASK_STAGING, TASK_STARTING, TASK_RUNNING, TASK_CLEANING, TASK_FINISHED, TASK_FAILED, TASK_KILLED, TASK_LOST, TASK_LOST_WHILE_DOWN |
| timestamp | long | optional |  |


## <a name="model-SingularityTaskId"></a> SingularityTaskId

| name | type | required | description |
|------|------|----------|-------------|
| requestId | string | optional |  |
| host | string | optional |  |
| deployId | string | optional |  |
| rackId | string | optional |  |
| instanceNo | int | optional |  |
| startedAt | long | optional |  |
| id | string | optional |  |


## <a name="model-SingularityTaskIdHistory"></a> SingularityTaskIdHistory

| name | type | required | description |
|------|------|----------|-------------|
| taskId | [SingularityTaskId](#model-SingularityTaskId) | optional |  |
| updatedAt | long | optional |  |
| lastTaskState | [ExtendedTaskState](#model-ExtendedTaskState) | optional |  |


## <a name="model-SingularityTaskRequest"></a> SingularityTaskRequest

| name | type | required | description |
|------|------|----------|-------------|
| deploy | [SingularityDeploy](#model-SingularityDeploy) | optional |  |
| request | [SingularityRequest](#model-SingularityRequest) | optional |  |
| pendingTask | [SingularityPendingTask](#model-SingularityPendingTask) | optional |  |


## <a name="model-SingularityVolume"></a> SingularityVolume

| name | type | required | description |
|------|------|----------|-------------|
| hostPath | string | optional |  |
| containerPath | string | optional |  |
| mode | [Mode](#model-Mode) | optional |  Allowable values: RW, RO |


## <a name="model-SingularityWebhook"></a> SingularityWebhook

| name | type | required | description |
|------|------|----------|-------------|
| type | [WebhookType](#model-WebhookType) | optional | Webhook type. Allowable values: TASK, REQUEST, DEPLOY |
| uri | string | optional | URI to POST to. |
| user | string | optional | User that created webhook. |
| timestamp | long | optional |  |
| id | string | optional | Unique ID for webhook. |


## <a name="model-SlaveID"></a> SlaveID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [SlaveID](#model-SlaveID) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$SlaveID&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$SlaveID&gt;) | optional |  |
| value | string | optional |  |
| initialized | boolean | optional |  |
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
| value | string | optional |  |
| initialized | boolean | optional |  |
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
| taskId | [TaskID](#model-TaskID) | optional |  |
| taskIdOrBuilder | [TaskIDOrBuilder](#model-TaskIDOrBuilder) | optional |  |
| parserForType | [com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskInfo&gt;](#model-com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskInfo&gt;) | optional |  |
| slaveIdOrBuilder | [SlaveIDOrBuilder](#model-SlaveIDOrBuilder) | optional |  |
| resourcesOrBuilderList | [List[? extends org.apache.mesos.Protos$ResourceOrBuilder]](#model-List[? extends org.apache.mesos.Protos$ResourceOrBuilder]) | optional |  |
| data | [ByteString](#model-ByteString) | optional |  |
| executor | [ExecutorInfo](#model-ExecutorInfo) | optional |  |
| containerOrBuilder | [ContainerInfoOrBuilder](#model-ContainerInfoOrBuilder) | optional |  |
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
| unknownFields | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| resourcesCount | int | optional |  |
| initializationErrorString | string | optional |  |


## <a name="model-UnknownFieldSet"></a> UnknownFieldSet

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | [UnknownFieldSet](#model-UnknownFieldSet) | optional |  |
| serializedSizeAsMessageSet | int | optional |  |
| parserForType | [Parser](#model-Parser) | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |


