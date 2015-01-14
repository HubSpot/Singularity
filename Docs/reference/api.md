# Singularity REST API

Version: 0.4.1-GRPN-5-SNAPSHOT

Endpoints:
- [`/api/deploys`](#apideploys) - Manages Singularity Deploys for existing requests
- [`/api/history`](#apihistory) - Manages historical data for tasks, requests, and deploys.
- [`/api/logs`](#apilogs) - Manages Singularity task logs stored in S3.
- [`/api/racks`](#apiracks) - Manages Singularity racks.
- [`/api/requests`](#apirequests) - Manages Singularity Requests, the parent object for any deployed task
- [`/api/sandbox`](#apisandbox) - Provides a proxy to Mesos sandboxes.
- [`/api/slaves`](#apislaves) - Manages Singularity slaves.
- [`/api/state`](#apistate) - Provides information about the current state of Singularity.
- [`/api/tasks`](#apitasks) - Manages Singularity tasks.
- [`/api/test`](#apitest) - Misc testing endpoints.
- [`/api/webhooks`](#apiwebhooks) - Manages Singularity webhooks.

Models:
- [`ByteString`](#bytestring)
- [`CommandInfo`](#commandinfo)
- [`CommandInfoOrBuilder`](#commandinfoorbuilder)
- [`ContainerInfo`](#containerinfo)
- [`ContainerInfoOrBuilder`](#containerinfoorbuilder)
- [`Descriptor`](#descriptor)
- [`DockerInfo`](#dockerinfo)
- [`DockerInfoOrBuilder`](#dockerinfoorbuilder)
- [`EmbeddedArtifact`](#embeddedartifact)
- [`Environment`](#environment)
- [`EnvironmentOrBuilder`](#environmentorbuilder)
- [`ExecutorData`](#executordata)
- [`ExecutorID`](#executorid)
- [`ExecutorIDOrBuilder`](#executoridorbuilder)
- [`ExecutorInfo`](#executorinfo)
- [`ExecutorInfoOrBuilder`](#executorinfoorbuilder)
- [`ExternalArtifact`](#externalartifact)
- [`FileDescriptor`](#filedescriptor)
- [`FileOptions`](#fileoptions)
- [`FrameworkID`](#frameworkid)
- [`FrameworkIDOrBuilder`](#frameworkidorbuilder)
- [`HTTP`](#http)
- [`HTTPOrBuilder`](#httporbuilder)
- [`HealthCheck`](#healthcheck)
- [`HealthCheckOrBuilder`](#healthcheckorbuilder)
- [`LoadBalancerRequestId`](#loadbalancerrequestid)
- [`MesosFileChunkObject`](#mesosfilechunkobject)
- [`MesosTaskStatisticsObject`](#mesostaskstatisticsobject)
- [`MessageOptions`](#messageoptions)
- [`Offer`](#offer)
- [`OfferID`](#offerid)
- [`OfferIDOrBuilder`](#offeridorbuilder)
- [`S3Artifact`](#s3artifact)
- [`SingularityContainerInfo`](#singularitycontainerinfo)
- [`SingularityDeploy`](#singularitydeploy)
- [`SingularityDeployHistory`](#singularitydeployhistory)
- [`SingularityDeployMarker`](#singularitydeploymarker)
- [`SingularityDeployRequest`](#singularitydeployrequest)
- [`SingularityDeployResult`](#singularitydeployresult)
- [`SingularityDeployStatistics`](#singularitydeploystatistics)
- [`SingularityDeployUpdate`](#singularitydeployupdate)
- [`SingularityDockerInfo`](#singularitydockerinfo)
- [`SingularityDockerPortMapping`](#singularitydockerportmapping)
- [`SingularityHostState`](#singularityhoststate)
- [`SingularityLoadBalancerUpdate`](#singularityloadbalancerupdate)
- [`SingularityPauseRequest`](#singularitypauserequest)
- [`SingularityPendingDeploy`](#singularitypendingdeploy)
- [`SingularityPendingRequest`](#singularitypendingrequest)
- [`SingularityPendingTask`](#singularitypendingtask)
- [`SingularityPendingTaskId`](#singularitypendingtaskid)
- [`SingularityRack`](#singularityrack)
- [`SingularityRequest`](#singularityrequest)
- [`SingularityRequestCleanup`](#singularityrequestcleanup)
- [`SingularityRequestDeployState`](#singularityrequestdeploystate)
- [`SingularityRequestHistory`](#singularityrequesthistory)
- [`SingularityRequestInstances`](#singularityrequestinstances)
- [`SingularityRequestParent`](#singularityrequestparent)
- [`SingularityResourceRequest`](#singularityresourcerequest)
- [`SingularitySandbox`](#singularitysandbox)
- [`SingularitySandboxFile`](#singularitysandboxfile)
- [`SingularitySlave`](#singularityslave)
- [`SingularityState`](#singularitystate)
- [`SingularityTask`](#singularitytask)
- [`SingularityTaskCleanup`](#singularitytaskcleanup)
- [`SingularityTaskCleanupResult`](#singularitytaskcleanupresult)
- [`SingularityTaskHealthcheckResult`](#singularitytaskhealthcheckresult)
- [`SingularityTaskHistory`](#singularitytaskhistory)
- [`SingularityTaskHistoryUpdate`](#singularitytaskhistoryupdate)
- [`SingularityTaskId`](#singularitytaskid)
- [`SingularityTaskIdHistory`](#singularitytaskidhistory)
- [`SingularityTaskRequest`](#singularitytaskrequest)
- [`SingularityVolume`](#singularityvolume)
- [`SingularityWebhook`](#singularitywebhook)
- [`SlaveID`](#slaveid)
- [`SlaveIDOrBuilder`](#slaveidorbuilder)
- [`TaskID`](#taskid)
- [`TaskIDOrBuilder`](#taskidorbuilder)
- [`TaskInfo`](#taskinfo)
- [`UnknownFieldSet`](#unknownfieldset)

- - -

## Endpoints
### /api/deploys
#### Overview
Manages Singularity Deploys for existing requests

#### **POST** `/api/deploys`

Start a new deployment for a Request


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | true |  | <a href="#SingularityDeployRequest">SingularityDeployRequest</a> |

###### Response
[SingularityRequestParent](#SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Deploy object is invalid | - |
| 409    | A current deploy is in progress. It may be canceled by calling DELETE | - |


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
[SingularityRequestParent](#SingularityRequestParent)


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
[List[SingularityPendingDeploy]](#SingularityPendingDeploy)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/history
#### Overview
Manages historical data for tasks, requests, and deploys.

#### **GET** `/api/history/request/{requestId}/deploy/{deployId}`

Retrieve the history for a specific deploy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID for deploy | string |
| deployId | true | Deploy ID | string |

###### Response
[SingularityDeployHistory](#SingularityDeployHistory)


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
[List[SingularityDeployHistory]](#SingularityDeployHistory)


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
[List[SingularityRequestHistory]](#SingularityRequestHistory)


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
[List[SingularityTaskIdHistory]](#SingularityTaskIdHistory)


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
[List[SingularityTaskIdHistory]](#SingularityTaskIdHistory)


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
[List[string]](#)


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
[SingularityTaskHistory](#SingularityTaskHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/logs
#### Overview
Manages Singularity task logs stored in S3.

#### **GET** `/api/logs/request/{requestId}`

Retrieve the list of logs stored in S3 for a specific request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to search for | string |

###### Response
[List[SingularityS3Log]](#SingularityS3Log)


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
[List[SingularityS3Log]](#SingularityS3Log)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/logs/task/{taskId}`

Retrieve the list of logs stored in S3 for a specific task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true | The task ID to search for | string |

###### Response
[List[SingularityS3Log]](#SingularityS3Log)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/racks
#### Overview
Manages Singularity racks.

#### **GET** `/api/racks/active`

Retrieve the list of active racks. A rack is active if it has one or more active slaves associated with it.


###### Parameters
- No parameters

###### Response
[List[SingularityRack]](#SingularityRack)


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
[List[SingularityRack]](#SingularityRack)


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
[List[SingularityRack]](#SingularityRack)


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
[](#)


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
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/racks/rack/{rackId}/decomissioning`

Undo the decomission operation on a specific decommissioning rack.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Decommissioned rack ID. | string |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/requests
#### Overview
Manages Singularity Requests, the parent object for any deployed task

#### **GET** `/api/requests`

Retrieve the list of all requests


###### Parameters
- No parameters

###### Response
[List[SingularityRequestParent]](#SingularityRequestParent)


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
| body | false | The Singularity request to create or update | <a href="#SingularityRequest">SingularityRequest</a> |

###### Response
[SingularityRequestParent](#SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Request object is invalid | - |
| 409    | Request object is being cleaned. Try again shortly | - |


- - -
#### **GET** `/api/requests/active`

Retrieve the list of active requests


###### Parameters
- No parameters

###### Response
[List[SingularityRequestParent]](#SingularityRequestParent)


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
[List[SingularityRequestParent]](#SingularityRequestParent)


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
[List[SingularityRequestParent]](#SingularityRequestParent)


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
[List[SingularityRequestParent]](#SingularityRequestParent)


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
[List[SingularityRequestCleanup]](#SingularityRequestCleanup)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/queued/pending`

Retrieve the list of pending requests


###### Parameters
- No parameters

###### Response
[List[SingularityPendingRequest]](#SingularityPendingRequest)


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
[SingularityRequestParent](#SingularityRequestParent)


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
[SingularityRequest](#SingularityRequest)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
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
[SingularityRequestParent](#SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


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
| body | false | Object to hold number of instances to request | <a href="#SingularityRequestInstances">SingularityRequestInstances</a> |

###### Response
[SingularityRequest](#SingularityRequest)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Posted object did not match Request ID | - |
| 404    | No Request with that ID | - |


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
| body | false | Pause Request Options | <a href="#SingularityPauseRequest">SingularityPauseRequest</a> |

###### Response
[SingularityRequestParent](#SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 409    | Request is already paused or being cleaned | - |


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
[SingularityRequestParent](#SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Singularity Request is not scheduled or one-off | - |


- - -
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
[SingularityRequestParent](#SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 409    | Request is not paused | - |


- - -
### /api/sandbox
#### Overview
Provides a proxy to Mesos sandboxes.

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
[SingularitySandbox](#SingularitySandbox)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
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
[MesosFileChunkObject](#MesosFileChunkObject)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/slaves
#### Overview
Manages Singularity slaves.

#### **GET** `/api/slaves/active`

Retrieve the list of active slaves.


###### Parameters
- No parameters

###### Response
[List[SingularitySlave]](#SingularitySlave)


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
[List[SingularitySlave]](#SingularitySlave)


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
[List[SingularitySlave]](#SingularitySlave)


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
[](#)


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
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/slaves/slave/{slaveId}/decomissioning`

Remove a specific decommissioning slave


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| slaveId | true |  | string |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/state
#### Overview
Provides information about the current state of Singularity.

#### **GET** `/api/state`

Retrieve information about the current state of Singularity.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| skipCache | false |  | boolean |
| includeRequestIds | false |  | boolean |

###### Response
[SingularityState](#SingularityState)


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
[List[string]](#)


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
[List[string]](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/tasks
#### Overview
Manages Singularity tasks.

#### **GET** `/api/tasks/active`

Retrieve the list of active tasks.


###### Parameters
- No parameters

###### Response
[List[SingularityTask]](#SingularityTask)


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
[List[SingularityTask]](#SingularityTask)


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
[List[SingularityTaskCleanup]](#SingularityTaskCleanup)


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
[List[SingularityTaskId]](#SingularityTaskId)


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
[List[SingularityTaskRequest]](#SingularityTaskRequest)


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
[List[SingularityPendingTaskId]](#SingularityPendingTaskId)


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
[List[SingularityTaskRequest]](#SingularityTaskRequest)


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
[SingularityTaskRequest](#SingularityTaskRequest)


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
[SingularityTaskCleanupResult](#SingularityTaskCleanupResult)


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
[SingularityTask](#SingularityTask)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/task/{taskId}/statistics`

Retrieve statistics about a specific active task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |

###### Response
[MesosTaskStatisticsObject](#MesosTaskStatisticsObject)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/test
#### Overview
Misc testing endpoints.

#### **POST** `/api/test/abort`

Abort the Mesos scheduler driver.


###### Parameters
- No parameters

###### Response
[](#)


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
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/notleader`

Make this instance of Singularity believe it&#39;s lost leadership.


###### Parameters
- No parameters

###### Response
[](#)


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
[](#)


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
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/stop`

Stop the Mesos scheduler driver.


###### Parameters
- No parameters

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/webhooks
#### Overview
Manages Singularity webhooks.

#### **GET** `/api/webhooks`

Retrieve a list of active webhooks.


###### Parameters
- No parameters

###### Response
[List[SingularityWebhook]](#SingularityWebhook)


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
| body | false |  | <a href="#SingularityWebhook">SingularityWebhook</a> |

###### Response
[string](#)


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
[List[SingularityDeployUpdate]](#SingularityDeployUpdate)


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
[List[SingularityRequestHistory]](#SingularityRequestHistory)


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
[List[SingularityTaskHistoryUpdate]](#SingularityTaskHistoryUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/webhooks/{webhookId}`

Delete a specific webhook.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | true |  | string |

###### Response
[string](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -

## Data Types

## ByteString

| name | type | required | description |
|------|------|----------|-------------|
| validUtf8 | boolean | optional |  |
| empty | boolean | optional |  |


## CommandInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#CommandInfo">CommandInfo</a> | optional |  |
| urisOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$CommandInfo$URIOrBuilder]">List[? extends org.apache.mesos.Protos$CommandInfo$URIOrBuilder]</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$CommandInfo&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$CommandInfo&gt;</a> | optional |  |
| urisCount | int | optional |  |
| argumentsCount | int | optional |  |
| argumentsList | Array[string] | optional |  |
| containerOrBuilder | <a href="#ContainerInfoOrBuilder">ContainerInfoOrBuilder</a> | optional |  |
| container | <a href="#ContainerInfo">ContainerInfo</a> | optional |  |
| user | string | optional |  |
| value | string | optional |  |
| initialized | boolean | optional |  |
| environment | <a href="#Environment">Environment</a> | optional |  |
| userBytes | <a href="#ByteString">ByteString</a> | optional |  |
| shell | boolean | optional |  |
| serializedSize | int | optional |  |
| urisList | <a href="#List[URI]">List[URI]</a> | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| environmentOrBuilder | <a href="#EnvironmentOrBuilder">EnvironmentOrBuilder</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |
| valueBytes | <a href="#ByteString">ByteString</a> | optional |  |
| initializationErrorString | string | optional |  |


## CommandInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| urisOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$CommandInfo$URIOrBuilder]">List[? extends org.apache.mesos.Protos$CommandInfo$URIOrBuilder]</a> | optional |  |
| argumentsCount | int | optional |  |
| urisCount | int | optional |  |
| argumentsList | Array[string] | optional |  |
| containerOrBuilder | <a href="#ContainerInfoOrBuilder">ContainerInfoOrBuilder</a> | optional |  |
| container | <a href="#ContainerInfo">ContainerInfo</a> | optional |  |
| user | string | optional |  |
| value | string | optional |  |
| environment | <a href="#Environment">Environment</a> | optional |  |
| userBytes | <a href="#ByteString">ByteString</a> | optional |  |
| shell | boolean | optional |  |
| urisList | <a href="#List[URI]">List[URI]</a> | optional |  |
| environmentOrBuilder | <a href="#EnvironmentOrBuilder">EnvironmentOrBuilder</a> | optional |  |
| valueBytes | <a href="#ByteString">ByteString</a> | optional |  |


## ContainerInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#ContainerInfo">ContainerInfo</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo&gt;</a> | optional |  |
| type | <a href="#Type">Type</a> | optional |  Allowable values: DOCKER, MESOS |
| hostname | string | optional |  |
| dockerOrBuilder | <a href="#DockerInfoOrBuilder">DockerInfoOrBuilder</a> | optional |  |
| initialized | boolean | optional |  |
| volumesCount | int | optional |  |
| serializedSize | int | optional |  |
| volumesList | <a href="#List[Volume]">List[Volume]</a> | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| hostnameBytes | <a href="#ByteString">ByteString</a> | optional |  |
| volumesOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$VolumeOrBuilder]">List[? extends org.apache.mesos.Protos$VolumeOrBuilder]</a> | optional |  |
| docker | <a href="#DockerInfo">DockerInfo</a> | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |
| initializationErrorString | string | optional |  |


## ContainerInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| type | <a href="#Type">Type</a> | optional |  Allowable values: DOCKER, MESOS |
| hostname | string | optional |  |
| dockerOrBuilder | <a href="#DockerInfoOrBuilder">DockerInfoOrBuilder</a> | optional |  |
| volumesCount | int | optional |  |
| volumesList | <a href="#List[Volume]">List[Volume]</a> | optional |  |
| hostnameBytes | <a href="#ByteString">ByteString</a> | optional |  |
| docker | <a href="#DockerInfo">DockerInfo</a> | optional |  |
| volumesOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$VolumeOrBuilder]">List[? extends org.apache.mesos.Protos$VolumeOrBuilder]</a> | optional |  |


## Descriptor

| name | type | required | description |
|------|------|----------|-------------|
| enumTypes | <a href="#List[EnumDescriptor]">List[EnumDescriptor]</a> | optional |  |
| fullName | string | optional |  |
| containingType | <a href="#Descriptor">Descriptor</a> | optional |  |
| file | <a href="#FileDescriptor">FileDescriptor</a> | optional |  |
| extensions | <a href="#List[FieldDescriptor]">List[FieldDescriptor]</a> | optional |  |
| options | <a href="#MessageOptions">MessageOptions</a> | optional |  |
| fields | <a href="#List[FieldDescriptor]">List[FieldDescriptor]</a> | optional |  |
| name | string | optional |  |
| index | int | optional |  |
| nestedTypes | <a href="#List[Descriptor]">List[Descriptor]</a> | optional |  |


## DockerInfo

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#DockerInfo">DockerInfo</a> | optional |  |
| portMappingsOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]">List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]</a> | optional |  |
| parametersList | <a href="#List[Parameter]">List[Parameter]</a> | optional |  |
| parametersOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$ParameterOrBuilder]">List[? extends org.apache.mesos.Protos$ParameterOrBuilder]</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo$DockerInfo&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ContainerInfo$DockerInfo&gt;</a> | optional |  |
| imageBytes | <a href="#ByteString">ByteString</a> | optional |  |
| initialized | boolean | optional |  |
| privileged | boolean | optional |  |
| portMappingsCount | int | optional |  |
| parametersCount | int | optional |  |
| serializedSize | int | optional |  |
| network | <a href="#Network">Network</a> | optional |  Allowable values: HOST, BRIDGE, NONE |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| portMappingsList | <a href="#List[PortMapping]">List[PortMapping]</a> | optional |  |
| image | string | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |
| initializationErrorString | string | optional |  |


## DockerInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| portMappingsOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]">List[? extends org.apache.mesos.Protos$ContainerInfo$DockerInfo$PortMappingOrBuilder]</a> | optional |  |
| parametersOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$ParameterOrBuilder]">List[? extends org.apache.mesos.Protos$ParameterOrBuilder]</a> | optional |  |
| parametersList | <a href="#List[Parameter]">List[Parameter]</a> | optional |  |
| imageBytes | <a href="#ByteString">ByteString</a> | optional |  |
| privileged | boolean | optional |  |
| parametersCount | int | optional |  |
| portMappingsCount | int | optional |  |
| network | <a href="#Network">Network</a> | optional |  Allowable values: HOST, BRIDGE, NONE |
| portMappingsList | <a href="#List[PortMapping]">List[PortMapping]</a> | optional |  |
| image | string | optional |  |


## EmbeddedArtifact

| name | type | required | description |
|------|------|----------|-------------|
| md5sum | string | optional |  |
| filename | string | optional |  |
| name | string | optional |  |
| content | <a href="#byte">Array[byte]</a> | optional |  |


## Environment

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#Environment">Environment</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Environment&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Environment&gt;</a> | optional |  |
| initialized | boolean | optional |  |
| variablesCount | int | optional |  |
| serializedSize | int | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| variablesOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$Environment$VariableOrBuilder]">List[? extends org.apache.mesos.Protos$Environment$VariableOrBuilder]</a> | optional |  |
| variablesList | <a href="#List[Variable]">List[Variable]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| initializationErrorString | string | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |


## EnvironmentOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| variablesCount | int | optional |  |
| variablesOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$Environment$VariableOrBuilder]">List[? extends org.apache.mesos.Protos$Environment$VariableOrBuilder]</a> | optional |  |
| variablesList | <a href="#List[Variable]">List[Variable]</a> | optional |  |


## ExecutorData

| name | type | required | description |
|------|------|----------|-------------|
| loggingExtraFields | <a href="#Map[string,string]">Map[string,string]</a> | optional |  |
| successfulExitCodes | Array[int] | optional |  |
| s3Artifacts | <a href="#S3Artifact">Array[S3Artifact]</a> | optional |  |
| embeddedArtifacts | <a href="#EmbeddedArtifact">Array[EmbeddedArtifact]</a> | optional |  |
| runningSentinel | string | optional |  |
| externalArtifacts | <a href="#ExternalArtifact">Array[ExternalArtifact]</a> | optional |  |
| user | string | optional |  |
| extraCmdLineArgs | Array[string] | optional |  |
| loggingTag | string | optional |  |
| sigKillProcessesAfterMillis | long | optional |  |
| maxTaskThreads | int | optional |  |
| cmd | string | optional |  |


## ExecutorID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#ExecutorID">ExecutorID</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorID&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorID&gt;</a> | optional |  |
| value | string | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | <a href="#ByteString">ByteString</a> | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |


## ExecutorIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | <a href="#ByteString">ByteString</a> | optional |  |


## ExecutorInfo

| name | type | required | description |
|------|------|----------|-------------|
| commandOrBuilder | <a href="#CommandInfoOrBuilder">CommandInfoOrBuilder</a> | optional |  |
| defaultInstanceForType | <a href="#ExecutorInfo">ExecutorInfo</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorInfo&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$ExecutorInfo&gt;</a> | optional |  |
| resourcesOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$ResourceOrBuilder]">List[? extends org.apache.mesos.Protos$ResourceOrBuilder]</a> | optional |  |
| data | <a href="#ByteString">ByteString</a> | optional |  |
| source | string | optional |  |
| containerOrBuilder | <a href="#ContainerInfoOrBuilder">ContainerInfoOrBuilder</a> | optional |  |
| container | <a href="#ContainerInfo">ContainerInfo</a> | optional |  |
| executorId | <a href="#ExecutorID">ExecutorID</a> | optional |  |
| initialized | boolean | optional |  |
| name | string | optional |  |
| nameBytes | <a href="#ByteString">ByteString</a> | optional |  |
| sourceBytes | <a href="#ByteString">ByteString</a> | optional |  |
| frameworkId | <a href="#FrameworkID">FrameworkID</a> | optional |  |
| command | <a href="#CommandInfo">CommandInfo</a> | optional |  |
| frameworkIdOrBuilder | <a href="#FrameworkIDOrBuilder">FrameworkIDOrBuilder</a> | optional |  |
| executorIdOrBuilder | <a href="#ExecutorIDOrBuilder">ExecutorIDOrBuilder</a> | optional |  |
| serializedSize | int | optional |  |
| resourcesList | <a href="#List[Resource]">List[Resource]</a> | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| resourcesCount | int | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |
| initializationErrorString | string | optional |  |


## ExecutorInfoOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| commandOrBuilder | <a href="#CommandInfoOrBuilder">CommandInfoOrBuilder</a> | optional |  |
| resourcesOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$ResourceOrBuilder]">List[? extends org.apache.mesos.Protos$ResourceOrBuilder]</a> | optional |  |
| data | <a href="#ByteString">ByteString</a> | optional |  |
| source | string | optional |  |
| containerOrBuilder | <a href="#ContainerInfoOrBuilder">ContainerInfoOrBuilder</a> | optional |  |
| container | <a href="#ContainerInfo">ContainerInfo</a> | optional |  |
| executorId | <a href="#ExecutorID">ExecutorID</a> | optional |  |
| name | string | optional |  |
| nameBytes | <a href="#ByteString">ByteString</a> | optional |  |
| sourceBytes | <a href="#ByteString">ByteString</a> | optional |  |
| frameworkId | <a href="#FrameworkID">FrameworkID</a> | optional |  |
| command | <a href="#CommandInfo">CommandInfo</a> | optional |  |
| frameworkIdOrBuilder | <a href="#FrameworkIDOrBuilder">FrameworkIDOrBuilder</a> | optional |  |
| executorIdOrBuilder | <a href="#ExecutorIDOrBuilder">ExecutorIDOrBuilder</a> | optional |  |
| resourcesList | <a href="#List[Resource]">List[Resource]</a> | optional |  |
| resourcesCount | int | optional |  |


## ExternalArtifact

| name | type | required | description |
|------|------|----------|-------------|
| md5sum | string | optional |  |
| url | string | optional |  |
| filename | string | optional |  |
| filesize | long | optional |  |
| name | string | optional |  |


## FileDescriptor

| name | type | required | description |
|------|------|----------|-------------|
| enumTypes | <a href="#List[EnumDescriptor]">List[EnumDescriptor]</a> | optional |  |
| publicDependencies | <a href="#List[FileDescriptor]">List[FileDescriptor]</a> | optional |  |
| extensions | <a href="#List[FieldDescriptor]">List[FieldDescriptor]</a> | optional |  |
| services | <a href="#List[ServiceDescriptor]">List[ServiceDescriptor]</a> | optional |  |
| options | <a href="#FileOptions">FileOptions</a> | optional |  |
| messageTypes | <a href="#List[Descriptor]">List[Descriptor]</a> | optional |  |
| name | string | optional |  |
| dependencies | <a href="#List[FileDescriptor]">List[FileDescriptor]</a> | optional |  |
| package | string | optional |  |


## FileOptions

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#FileOptions">FileOptions</a> | optional |  |
| javaMultipleFiles | boolean | optional |  |
| optimizeFor | <a href="#OptimizeMode">OptimizeMode</a> | optional |  Allowable values: SPEED, CODE_SIZE, LITE_RUNTIME |
| parserForType | <a href="#com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$FileOptions&gt;">com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$FileOptions&gt;</a> | optional |  |
| javaPackageBytes | <a href="#ByteString">ByteString</a> | optional |  |
| goPackageBytes | <a href="#ByteString">ByteString</a> | optional |  |
| uninterpretedOptionCount | int | optional |  |
| javaGenericServices | boolean | optional |  |
| javaOuterClassnameBytes | <a href="#ByteString">ByteString</a> | optional |  |
| initialized | boolean | optional |  |
| javaOuterClassname | string | optional |  |
| serializedSize | int | optional |  |
| pyGenericServices | boolean | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| uninterpretedOptionList | <a href="#List[UninterpretedOption]">List[UninterpretedOption]</a> | optional |  |
| javaPackage | string | optional |  |
| goPackage | string | optional |  |
| uninterpretedOptionOrBuilderList | <a href="#List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]">List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]</a> | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |
| javaGenerateEqualsAndHash | boolean | optional |  |
| initializationErrorString | string | optional |  |
| ccGenericServices | boolean | optional |  |


## FrameworkID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#FrameworkID">FrameworkID</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$FrameworkID&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$FrameworkID&gt;</a> | optional |  |
| value | string | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | <a href="#ByteString">ByteString</a> | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |


## FrameworkIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | <a href="#ByteString">ByteString</a> | optional |  |


## HTTP

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#HTTP">HTTP</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$HealthCheck$HTTP&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$HealthCheck$HTTP&gt;</a> | optional |  |
| pathBytes | <a href="#ByteString">ByteString</a> | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| statusesCount | int | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| path | string | optional |  |
| port | int | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |
| statusesList | Array[int] | optional |  |
| initializationErrorString | string | optional |  |


## HTTPOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| pathBytes | <a href="#ByteString">ByteString</a> | optional |  |
| statusesCount | int | optional |  |
| port | int | optional |  |
| path | string | optional |  |
| statusesList | Array[int] | optional |  |


## HealthCheck

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#HealthCheck">HealthCheck</a> | optional |  |
| commandOrBuilder | <a href="#CommandInfoOrBuilder">CommandInfoOrBuilder</a> | optional |  |
| gracePeriodSeconds | double | optional |  |
| httpOrBuilder | <a href="#HTTPOrBuilder">HTTPOrBuilder</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$HealthCheck&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$HealthCheck&gt;</a> | optional |  |
| consecutiveFailures | int | optional |  |
| intervalSeconds | double | optional |  |
| initialized | boolean | optional |  |
| command | <a href="#CommandInfo">CommandInfo</a> | optional |  |
| serializedSize | int | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| timeoutSeconds | double | optional |  |
| http | <a href="#HTTP">HTTP</a> | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |
| delaySeconds | double | optional |  |
| initializationErrorString | string | optional |  |


## HealthCheckOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| gracePeriodSeconds | double | optional |  |
| commandOrBuilder | <a href="#CommandInfoOrBuilder">CommandInfoOrBuilder</a> | optional |  |
| httpOrBuilder | <a href="#HTTPOrBuilder">HTTPOrBuilder</a> | optional |  |
| consecutiveFailures | int | optional |  |
| intervalSeconds | double | optional |  |
| command | <a href="#CommandInfo">CommandInfo</a> | optional |  |
| timeoutSeconds | double | optional |  |
| http | <a href="#HTTP">HTTP</a> | optional |  |
| delaySeconds | double | optional |  |


## LoadBalancerRequestId

| name | type | required | description |
|------|------|----------|-------------|
| requestType | <a href="#LoadBalancerRequestType">LoadBalancerRequestType</a> | optional |  Allowable values: ADD, REMOVE, DEPLOY |
| attemptNumber | int | optional |  |
| id | string | optional |  |


## MesosFileChunkObject

| name | type | required | description |
|------|------|----------|-------------|
| data | string | optional |  |
| offset | long | optional |  |


## MesosTaskStatisticsObject

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


## MessageOptions

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#MessageOptions">MessageOptions</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$MessageOptions&gt;">com.google.protobuf.Parser&lt;com.google.protobuf.DescriptorProtos$MessageOptions&gt;</a> | optional |  |
| uninterpretedOptionCount | int | optional |  |
| initialized | boolean | optional |  |
| noStandardDescriptorAccessor | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| messageSetWireFormat | boolean | optional |  |
| uninterpretedOptionList | <a href="#List[UninterpretedOption]">List[UninterpretedOption]</a> | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |
| uninterpretedOptionOrBuilderList | <a href="#List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]">List[? extends com.google.protobuf.DescriptorProtos$UninterpretedOptionOrBuilder]</a> | optional |  |
| initializationErrorString | string | optional |  |


## Offer

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#Offer">Offer</a> | optional |  |
| executorIdsOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$ExecutorIDOrBuilder]">List[? extends org.apache.mesos.Protos$ExecutorIDOrBuilder]</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Offer&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$Offer&gt;</a> | optional |  |
| slaveIdOrBuilder | <a href="#SlaveIDOrBuilder">SlaveIDOrBuilder</a> | optional |  |
| executorIdsCount | int | optional |  |
| resourcesOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$ResourceOrBuilder]">List[? extends org.apache.mesos.Protos$ResourceOrBuilder]</a> | optional |  |
| executorIdsList | <a href="#List[ExecutorID]">List[ExecutorID]</a> | optional |  |
| hostname | string | optional |  |
| attributesCount | int | optional |  |
| initialized | boolean | optional |  |
| attributesList | <a href="#List[Attribute]">List[Attribute]</a> | optional |  |
| idOrBuilder | <a href="#OfferIDOrBuilder">OfferIDOrBuilder</a> | optional |  |
| frameworkId | <a href="#FrameworkID">FrameworkID</a> | optional |  |
| frameworkIdOrBuilder | <a href="#FrameworkIDOrBuilder">FrameworkIDOrBuilder</a> | optional |  |
| serializedSize | int | optional |  |
| resourcesList | <a href="#List[Resource]">List[Resource]</a> | optional |  |
| slaveId | <a href="#SlaveID">SlaveID</a> | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| hostnameBytes | <a href="#ByteString">ByteString</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| attributesOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$AttributeOrBuilder]">List[? extends org.apache.mesos.Protos$AttributeOrBuilder]</a> | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |
| resourcesCount | int | optional |  |
| id | <a href="#OfferID">OfferID</a> | optional |  |
| initializationErrorString | string | optional |  |


## OfferID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#OfferID">OfferID</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$OfferID&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$OfferID&gt;</a> | optional |  |
| value | string | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | <a href="#ByteString">ByteString</a> | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |


## OfferIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | <a href="#ByteString">ByteString</a> | optional |  |


## S3Artifact

| name | type | required | description |
|------|------|----------|-------------|
| s3Bucket | string | optional |  |
| md5sum | string | optional |  |
| filename | string | optional |  |
| filesize | long | optional |  |
| s3ObjectKey | string | optional |  |
| name | string | optional |  |


## SingularityContainerInfo

| name | type | required | description |
|------|------|----------|-------------|
| type | <a href="#Type">Type</a> | optional |  Allowable values: DOCKER, MESOS |
| volumes | <a href="#SingularityVolume">Array[SingularityVolume]</a> | optional |  |
| docker | <a href="#SingularityDockerInfo">SingularityDockerInfo</a> | optional |  |


## SingularityDeploy

| name | type | required | description |
|------|------|----------|-------------|
| customExecutorId | string | optional | Custom Mesos executor id. |
| resources | <a href="#com.hubspot.mesos.Resources">com.hubspot.mesos.Resources</a> | optional | Resources required for this deploy. |
| uris | Array[string] | optional | List of URIs to download before executing the deploy command. |
| containerInfo | <a href="#SingularityContainerInfo">SingularityContainerInfo</a> | optional | Container information for deployment into a container. |
| arguments | Array[string] | optional | Command arguments. |
| serviceBasePath | string | optional | The base path for the API exposed by the deploy. Used in conjunction with the Load balancer API. |
| customExecutorSource | string | optional | Custom Mesos executor source. |
| metadata | <a href="#Map[string,string]">Map[string,string]</a> | optional | Map of metadata key/value pairs associated with the deployment. |
| healthcheckTimeoutSeconds | long | optional | Health check timeout in seconds. |
| healthcheckUri | string | optional | Deployment Healthcheck URI. |
| requestId | string | required | Singularity Request Id which is associated with this deploy. |
| loadBalancerGroups | Array[string] | optional | List of load balancer groups associated with this deployment. |
| skipHealthchecksOnDeploy | boolean | optional | Allows skipping of health checks when deploying. |
| healthcheckIntervalSeconds | long | optional | Health check interval in seconds. |
| command | string | optional | Command to execute for this deployment. |
| requestedResources | <a href="#SingularityResourceRequest">Array[SingularityResourceRequest]</a> | optional | Resources required for this deploy. |
| executorData | <a href="#ExecutorData">ExecutorData</a> | optional | Executor specific information |
| timestamp | long | optional | Deploy timestamp. |
| considerHealthyAfterRunningForSeconds | long | optional | Number of seconds that a service must be healthy to consider the deployment to be successful. |
| loadBalancerOptions | <a href="#Map[string,Object]">Map[string,Object]</a> | optional | Map (Key/Value) of options for the load balancer. |
| requestedAttributes | <a href="#Map[string,string]">Map[string,string]</a> | optional | Attributes required for this deploy. |
| customExecutorCmd | string | optional | Custom Mesos executor |
| env | <a href="#Map[string,string]">Map[string,string]</a> | optional | Map of environment variable definitions. |
| version | string | optional | Deploy version |
| id | string | required | Singularity deploy id. |
| deployHealthTimeoutSeconds | long | optional | Number of seconds that singularity waits for this service to become healthy. |


## SingularityDeployHistory

| name | type | required | description |
|------|------|----------|-------------|
| deploy | <a href="#SingularityDeploy">SingularityDeploy</a> | optional |  |
| deployStatistics | <a href="#SingularityDeployStatistics">SingularityDeployStatistics</a> | optional |  |
| deployResult | <a href="#SingularityDeployResult">SingularityDeployResult</a> | optional |  |
| deployMarker | <a href="#SingularityDeployMarker">SingularityDeployMarker</a> | optional |  |


## SingularityDeployMarker

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| requestId | string | optional |  |
| timestamp | long | optional |  |
| deployId | string | optional |  |


## SingularityDeployRequest

| name | type | required | description |
|------|------|----------|-------------|
| unpauseOnSuccessfulDeploy | boolean | optional | If deploy is successful, also unpause the request. |
| deploy | <a href="#SingularityDeploy">SingularityDeploy</a> | required | The Singularity deploy object |
| user | string | optional | User owning this deploy. |


## SingularityDeployResult

| name | type | required | description |
|------|------|----------|-------------|
| lbUpdate | <a href="#SingularityLoadBalancerUpdate">SingularityLoadBalancerUpdate</a> | optional |  |
| deployState | <a href="#DeployState">DeployState</a> | optional |  Allowable values: SUCCEEDED, FAILED_INTERNAL_STATE, CANCELING, WAITING, OVERDUE, FAILED, CANCELED |
| message | string | optional |  |
| timestamp | long | optional |  |


## SingularityDeployStatistics

| name | type | required | description |
|------|------|----------|-------------|
| lastTaskState | <a href="#ExtendedTaskState">ExtendedTaskState</a> | optional |  |
| numFailures | int | optional |  |
| numTasks | int | optional |  |
| averageRuntimeMillis | long | optional |  |
| lastFinishAt | long | optional |  |
| requestId | string | optional |  |
| deployId | string | optional |  |
| numSequentialRetries | int | optional |  |
| numSuccess | int | optional |  |
| instanceSequentialFailureTimestamps | <a href="#com.google.common.collect.ListMultimap&lt;java.lang.Integer, java.lang.Long&gt;">com.google.common.collect.ListMultimap&lt;java.lang.Integer, java.lang.Long&gt;</a> | optional |  |


## SingularityDeployUpdate

| name | type | required | description |
|------|------|----------|-------------|
| deploy | <a href="#SingularityDeploy">SingularityDeploy</a> | optional |  |
| deployResult | <a href="#SingularityDeployResult">SingularityDeployResult</a> | optional |  |
| eventType | <a href="#DeployEventType">DeployEventType</a> | optional |  Allowable values: STARTING, FINISHED |
| deployMarker | <a href="#SingularityDeployMarker">SingularityDeployMarker</a> | optional |  |


## SingularityDockerInfo

| name | type | required | description |
|------|------|----------|-------------|
| privileged | boolean | optional |  |
| network | <a href="#Network">Network</a> | optional |  |
| portMappings | <a href="#SingularityDockerPortMapping">Array[SingularityDockerPortMapping]</a> | optional |  |
| image | string | optional |  |


## SingularityDockerPortMapping

| name | type | required | description |
|------|------|----------|-------------|
| hostPort | int | optional |  |
| containerPort | int | optional |  |
| containerPortType | <a href="#SingularityPortMappingType">SingularityPortMappingType</a> | optional |  Allowable values: LITERAL, FROM_OFFER |
| protocol | string | optional |  |
| hostPortType | <a href="#SingularityPortMappingType">SingularityPortMappingType</a> | optional |  Allowable values: LITERAL, FROM_OFFER |


## SingularityHostState

| name | type | required | description |
|------|------|----------|-------------|
| hostAddress | string | optional |  |
| hostname | string | optional |  |
| driverStatus | string | optional |  |
| master | boolean | optional |  |
| mesosMaster | string | optional |  |
| uptime | long | optional |  |
| millisSinceLastOffer | long | optional |  |


## SingularityLoadBalancerUpdate

| name | type | required | description |
|------|------|----------|-------------|
| loadBalancerState | <a href="#BaragonRequestState">BaragonRequestState</a> | optional |  Allowable values: UNKNOWN, FAILED, WAITING, SUCCESS, CANCELING, CANCELED |
| loadBalancerRequestId | <a href="#LoadBalancerRequestId">LoadBalancerRequestId</a> | optional |  |
| uri | string | optional |  |
| method | <a href="#LoadBalancerMethod">LoadBalancerMethod</a> | optional |  Allowable values: PRE_ENQUEUE, ENQUEUE, CHECK_STATE, CANCEL |
| message | string | optional |  |
| timestamp | long | optional |  |


## SingularityPauseRequest

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| killTasks | boolean | optional |  |


## SingularityPendingDeploy

| name | type | required | description |
|------|------|----------|-------------|
| currentDeployState | <a href="#DeployState">DeployState</a> | optional |  Allowable values: SUCCEEDED, FAILED_INTERNAL_STATE, CANCELING, WAITING, OVERDUE, FAILED, CANCELED |
| lastLoadBalancerUpdate | <a href="#SingularityLoadBalancerUpdate">SingularityLoadBalancerUpdate</a> | optional |  |
| deployMarker | <a href="#SingularityDeployMarker">SingularityDeployMarker</a> | optional |  |


## SingularityPendingRequest

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| requestId | string | optional |  |
| cmdLineArgs | string | optional |  |
| timestamp | long | optional |  |
| deployId | string | optional |  |
| pendingType | <a href="#PendingType">PendingType</a> | optional |  Allowable values: IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP |


## SingularityPendingTask

| name | type | required | description |
|------|------|----------|-------------|
| pendingTaskId | <a href="#SingularityPendingTaskId">SingularityPendingTaskId</a> | optional |  |
| maybeCmdLineArgs | string | optional |  |


## SingularityPendingTaskId

| name | type | required | description |
|------|------|----------|-------------|
| nextRunAt | long | optional |  |
| requestId | string | optional |  |
| deployId | string | optional |  |
| pendingType | <a href="#PendingType">PendingType</a> | optional |  Allowable values: IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP |
| instanceNo | int | optional |  |
| createdAt | long | optional |  |
| id | string | optional |  |


## SingularityRack

| name | type | required | description |
|------|------|----------|-------------|
| deadAt | long | optional |  |
| decomissionedAt | long | optional |  |
| state | <a href="#SingularityMachineState">SingularityMachineState</a> | optional |  Allowable values: ACTIVE, DECOMISSIONING, DECOMISSIONED, DEAD |
| decomissioningBy | string | optional |  |
| decomissioningAt | long | optional |  |
| firstSeenAt | long | optional |  |
| id | string | optional |  |


## SingularityRequest

| name | type | required | description |
|------|------|----------|-------------|
| schedule | string | optional |  |
| rackAffinity | Array[string] | optional |  |
| daemon | boolean | optional |  |
| slavePlacement | <a href="#SlavePlacement">SlavePlacement</a> | optional |  |
| rackSensitive | boolean | optional |  |
| owners | Array[string] | optional |  |
| quartzSchedule | string | optional |  |
| scheduledExpectedRuntimeMillis | long | optional |  |
| loadBalanced | boolean | optional |  |
| numRetriesOnFailure | int | optional |  |
| killOldNonLongRunningTasksAfterMillis | long | optional |  |
| instances | int | optional |  |
| scheduleType | <a href="#ScheduleType">ScheduleType</a> | optional |  |
| id | string | optional |  |


## SingularityRequestCleanup

| name | type | required | description |
|------|------|----------|-------------|
| requestId | string | optional |  |
| user | string | optional |  |
| killTasks | boolean | optional |  |
| cleanupType | <a href="#RequestCleanupType">RequestCleanupType</a> | optional |  Allowable values: DELETING, PAUSING, BOUNCE |
| timestamp | long | optional |  |
| deployId | string | optional |  |


## SingularityRequestDeployState

| name | type | required | description |
|------|------|----------|-------------|
| pendingDeploy | <a href="#SingularityDeployMarker">SingularityDeployMarker</a> | optional |  |
| requestId | string | optional |  |
| activeDeploy | <a href="#SingularityDeployMarker">SingularityDeployMarker</a> | optional |  |


## SingularityRequestHistory

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| request | <a href="#SingularityRequest">SingularityRequest</a> | optional |  |
| eventType | <a href="#RequestHistoryType">RequestHistoryType</a> | optional |  Allowable values: CREATED, UPDATED, DELETED, PAUSED, UNPAUSED, ENTERED_COOLDOWN, EXITED_COOLDOWN, FINISHED, DEPLOYED_TO_UNPAUSE |
| createdAt | long | optional |  |


## SingularityRequestInstances

| name | type | required | description |
|------|------|----------|-------------|
| instances | int | optional |  |
| id | string | optional |  |


## SingularityRequestParent

| name | type | required | description |
|------|------|----------|-------------|
| state | <a href="#RequestState">RequestState</a> | optional |  Allowable values: ACTIVE, DELETED, PAUSED, SYSTEM_COOLDOWN, FINISHED, DEPLOYING_TO_UNPAUSE |
| pendingDeploy | <a href="#SingularityDeploy">SingularityDeploy</a> | optional |  |
| activeDeploy | <a href="#SingularityDeploy">SingularityDeploy</a> | optional |  |
| request | <a href="#SingularityRequest">SingularityRequest</a> | optional |  |
| pendingDeployState | <a href="#SingularityPendingDeploy">SingularityPendingDeploy</a> | optional |  |
| requestDeployState | <a href="#SingularityRequestDeployState">SingularityRequestDeployState</a> | optional |  |


## SingularityResourceRequest

| name | type | required | description |
|------|------|----------|-------------|
| value | <a href="#Object">Object</a> | optional |  |
| name | string | optional |  |


## SingularitySandbox

| name | type | required | description |
|------|------|----------|-------------|
| slaveHostname | string | optional | Hostname of tasks's slave |
| files | <a href="#SingularitySandboxFile">Array[SingularitySandboxFile]</a> | optional | List of files inside sandbox |
| currentDirectory | string | optional | Current directory |
| fullPathToRoot | string | optional | Full path to the root of the Mesos task sandbox |


## SingularitySandboxFile

| name | type | required | description |
|------|------|----------|-------------|
| size | long | optional | File size (in bytes) |
| mode | string | optional | File mode |
| mtime | long | optional | Last modified time |
| name | string | optional | Filename |


## SingularitySlave

| name | type | required | description |
|------|------|----------|-------------|
| deadAt | long | optional |  |
| decomissionedAt | long | optional |  |
| state | <a href="#SingularityMachineState">SingularityMachineState</a> | optional |  Allowable values: ACTIVE, DECOMISSIONING, DECOMISSIONED, DEAD |
| host | string | optional | Slave hostname |
| decomissioningBy | string | optional |  |
| decomissioningAt | long | optional |  |
| rackId | string | optional | Slave rack ID |
| firstSeenAt | long | optional |  |
| id | string | optional |  |


## SingularityState

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
| hostStates | <a href="#SingularityHostState">Array[SingularityHostState]</a> | optional |  |
| allRequests | int | optional |  |
| underProvisionedRequests | int | optional |  |
| decomissioningSlaves | int | optional |  |
| oldestDeploy | long | optional |  |
| scheduledTasks | int | optional |  |
| underProvisionedRequestIds | Array[string] | optional |  |


## SingularityTask

| name | type | required | description |
|------|------|----------|-------------|
| taskId | <a href="#SingularityTaskId">SingularityTaskId</a> | optional |  |
| taskRequest | <a href="#SingularityTaskRequest">SingularityTaskRequest</a> | optional |  |
| offer | <a href="#Offer">Offer</a> | optional |  |
| mesosTask | <a href="#TaskInfo">TaskInfo</a> | optional |  |


## SingularityTaskCleanup

| name | type | required | description |
|------|------|----------|-------------|
| taskId | <a href="#SingularityTaskId">SingularityTaskId</a> | optional |  |
| user | string | optional |  |
| cleanupType | <a href="#TaskCleanupType">TaskCleanupType</a> | optional |  Allowable values: USER_REQUESTED, DECOMISSIONING, SCALING_DOWN, BOUNCING, DEPLOY_FAILED, NEW_DEPLOY_SUCCEEDED, DEPLOY_CANCELED, UNHEALTHY_NEW_TASK, OVERDUE_NEW_TASK |
| timestamp | long | optional |  |


## SingularityTaskCleanupResult

| name | type | required | description |
|------|------|----------|-------------|
| result | <a href="#SingularityCreateResult">SingularityCreateResult</a> | optional |  Allowable values: CREATED, EXISTED |
| task | <a href="#SingularityTask">SingularityTask</a> | optional |  |


## SingularityTaskHealthcheckResult

| name | type | required | description |
|------|------|----------|-------------|
| taskId | <a href="#SingularityTaskId">SingularityTaskId</a> | optional |  |
| durationMillis | long | optional |  |
| errorMessage | string | optional |  |
| statusCode | int | optional |  |
| timestamp | long | optional |  |
| responseBody | string | optional |  |


## SingularityTaskHistory

| name | type | required | description |
|------|------|----------|-------------|
| directory | string | optional |  |
| task | <a href="#SingularityTask">SingularityTask</a> | optional |  |
| healthcheckResults | <a href="#SingularityTaskHealthcheckResult">Array[SingularityTaskHealthcheckResult]</a> | optional |  |
| loadBalancerUpdates | <a href="#SingularityLoadBalancerUpdate">Array[SingularityLoadBalancerUpdate]</a> | optional |  |
| taskUpdates | <a href="#SingularityTaskHistoryUpdate">Array[SingularityTaskHistoryUpdate]</a> | optional |  |


## SingularityTaskHistoryUpdate

| name | type | required | description |
|------|------|----------|-------------|
| taskId | <a href="#SingularityTaskId">SingularityTaskId</a> | optional |  |
| statusMessage | string | optional |  |
| taskState | <a href="#ExtendedTaskState">ExtendedTaskState</a> | optional |  Allowable values: TASK_LAUNCHED, TASK_STAGING, TASK_STARTING, TASK_RUNNING, TASK_CLEANING, TASK_FINISHED, TASK_FAILED, TASK_KILLED, TASK_LOST, TASK_LOST_WHILE_DOWN |
| timestamp | long | optional |  |


## SingularityTaskId

| name | type | required | description |
|------|------|----------|-------------|
| requestId | string | optional |  |
| host | string | optional |  |
| deployId | string | optional |  |
| rackId | string | optional |  |
| instanceNo | int | optional |  |
| startedAt | long | optional |  |
| id | string | optional |  |


## SingularityTaskIdHistory

| name | type | required | description |
|------|------|----------|-------------|
| taskId | <a href="#SingularityTaskId">SingularityTaskId</a> | optional |  |
| updatedAt | long | optional |  |
| lastTaskState | <a href="#ExtendedTaskState">ExtendedTaskState</a> | optional |  |


## SingularityTaskRequest

| name | type | required | description |
|------|------|----------|-------------|
| deploy | <a href="#SingularityDeploy">SingularityDeploy</a> | optional |  |
| request | <a href="#SingularityRequest">SingularityRequest</a> | optional |  |
| pendingTask | <a href="#SingularityPendingTask">SingularityPendingTask</a> | optional |  |


## SingularityVolume

| name | type | required | description |
|------|------|----------|-------------|
| hostPath | string | optional |  |
| containerPath | string | optional |  |
| mode | <a href="#Mode">Mode</a> | optional |  Allowable values: RW, RO |


## SingularityWebhook

| name | type | required | description |
|------|------|----------|-------------|
| type | <a href="#WebhookType">WebhookType</a> | optional | Webhook type. Allowable values: TASK, REQUEST, DEPLOY |
| uri | string | optional | URI to POST to. |
| user | string | optional | User that created webhook. |
| timestamp | long | optional |  |
| id | string | optional | Unique ID for webhook. |


## SlaveID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#SlaveID">SlaveID</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$SlaveID&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$SlaveID&gt;</a> | optional |  |
| value | string | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | <a href="#ByteString">ByteString</a> | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |


## SlaveIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | <a href="#ByteString">ByteString</a> | optional |  |


## TaskID

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#TaskID">TaskID</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskID&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskID&gt;</a> | optional |  |
| value | string | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| initializationErrorString | string | optional |  |
| valueBytes | <a href="#ByteString">ByteString</a> | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |


## TaskIDOrBuilder

| name | type | required | description |
|------|------|----------|-------------|
| value | string | optional |  |
| valueBytes | <a href="#ByteString">ByteString</a> | optional |  |


## TaskInfo

| name | type | required | description |
|------|------|----------|-------------|
| commandOrBuilder | <a href="#CommandInfoOrBuilder">CommandInfoOrBuilder</a> | optional |  |
| defaultInstanceForType | <a href="#TaskInfo">TaskInfo</a> | optional |  |
| taskIdOrBuilder | <a href="#TaskIDOrBuilder">TaskIDOrBuilder</a> | optional |  |
| taskId | <a href="#TaskID">TaskID</a> | optional |  |
| parserForType | <a href="#com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskInfo&gt;">com.google.protobuf.Parser&lt;org.apache.mesos.Protos$TaskInfo&gt;</a> | optional |  |
| slaveIdOrBuilder | <a href="#SlaveIDOrBuilder">SlaveIDOrBuilder</a> | optional |  |
| resourcesOrBuilderList | <a href="#List[? extends org.apache.mesos.Protos$ResourceOrBuilder]">List[? extends org.apache.mesos.Protos$ResourceOrBuilder]</a> | optional |  |
| data | <a href="#ByteString">ByteString</a> | optional |  |
| executor | <a href="#ExecutorInfo">ExecutorInfo</a> | optional |  |
| containerOrBuilder | <a href="#ContainerInfoOrBuilder">ContainerInfoOrBuilder</a> | optional |  |
| executorOrBuilder | <a href="#ExecutorInfoOrBuilder">ExecutorInfoOrBuilder</a> | optional |  |
| container | <a href="#ContainerInfo">ContainerInfo</a> | optional |  |
| healthCheckOrBuilder | <a href="#HealthCheckOrBuilder">HealthCheckOrBuilder</a> | optional |  |
| initialized | boolean | optional |  |
| name | string | optional |  |
| nameBytes | <a href="#ByteString">ByteString</a> | optional |  |
| command | <a href="#CommandInfo">CommandInfo</a> | optional |  |
| healthCheck | <a href="#HealthCheck">HealthCheck</a> | optional |  |
| serializedSize | int | optional |  |
| resourcesList | <a href="#List[Resource]">List[Resource]</a> | optional |  |
| slaveId | <a href="#SlaveID">SlaveID</a> | optional |  |
| allFields | <a href="#Map[FieldDescriptor,Object]">Map[FieldDescriptor,Object]</a> | optional |  |
| descriptorForType | <a href="#Descriptor">Descriptor</a> | optional |  |
| unknownFields | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |
| resourcesCount | int | optional |  |
| initializationErrorString | string | optional |  |


## UnknownFieldSet

| name | type | required | description |
|------|------|----------|-------------|
| defaultInstanceForType | <a href="#UnknownFieldSet">UnknownFieldSet</a> | optional |  |
| serializedSizeAsMessageSet | int | optional |  |
| parserForType | <a href="#Parser">Parser</a> | optional |  |
| initialized | boolean | optional |  |
| serializedSize | int | optional |  |


