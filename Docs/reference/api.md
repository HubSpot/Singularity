# Singularity REST API

Version: 0.4.0-SNAPSHOT

Endpoints:
- [`/api/deploys`](#apideploys) - Manages Singularity deploys for existing requests.
- [`/api/history`](#apihistory) - Manages historical data for tasks, requests, and deploys.
- [`/api/logs`](#apilogs) - Manages Singularity task logs stored in S3.
- [`/api/racks`](#apiracks) - Manages Singularity racks.
- [`/api/requests`](#apirequests) - Manages Singularity requests.
- [`/api/sandbox`](#apisandbox) - Provides a proxy to Mesos sandboxes.
- [`/api/slaves`](#apislaves) - Manages Singularity slaves.
- [`/api/state`](#apistate) - Provides information about the current state of Singularity.
- [`/api/tasks`](#apitasks) - Manages Singularity tasks.
- [`/api/test`](#apitest) - Misc testing endpoints.
- [`/api/webhooks`](#apiwebhooks) - Manages Singularity webhooks.

Models:
- [`SingularityDeploy`](#singularitydeploy)
- [`SingularityDeployRequest`](#singularitydeployrequest)
- [`SingularityRequest`](#singularityrequest)
- [`SingularityRequestInstances`](#singularityrequestinstances)
- [`SingularityWebhook`](#singularitywebhook)

Enums:
- *none*

- - -

## Endpoints
<a name="#api-0"></a>
### /api/deploys
#### Overview
Manages Singularity deploys for existing requests.

#### **POST** `/api/deploys`

Create a new deployment.


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | true |  | <a href="#SingularityDeployRequest">SingularityDeployRequest</a> |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 200    | Deploy successfully scheduled. | - |
| 400    | Deploy object is invalid. | - |
| 409    | A current deploy is in progress. | - |


- - -
#### **GET** `/api/deploys/pending`

Retrieve the list of pending deploys.


###### Parameters
- No parameters

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/deploys/deploy/{deployId}/request/{requestId}`

Delete a pending deployment from a request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Singularity Request Id from which the deployment is removed. | string |
| deployId | true | The Singularity Deploy Id that should be removed. | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | The user which executes the delete request. | <a href="#UNKNOWN[string]">UNKNOWN[string]</a> |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Deploy is not pending or not present. | - |
| 200    | Pending deploy successfully removed. | - |


- - -
<a name="#api-1"></a>
### /api/history
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
[](#)


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
[](#)


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
[](#)


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
[](#)


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
[](#)


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
[](#)


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
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
<a name="#api-2"></a>
### /api/logs
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
[](#)


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
[](#)


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
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
<a name="#api-3"></a>
### /api/racks
#### Overview
Manages Singularity racks.

#### **GET** `/api/racks/dead`

Retrieve the list of dead racks. A rack is dead if it has zero active slaves.


###### Parameters
- No parameters

###### Response
[](#)


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
[](#)


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
[](#)


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
| user | false | Username of person requestin the decommisioning. | <a href="#UNKNOWN[string]">UNKNOWN[string]</a> |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
<a name="#api-4"></a>
### /api/requests
#### Overview
Manages Singularity requests.

#### **GET** `/api/requests`

Retrieve the list of all requests.


###### Parameters
- No parameters

###### Response
[](#)


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
| user | false | Username of the person requesting to create or update. | <a href="#UNKNOWN[string]">UNKNOWN[string]</a> |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | The Singularity request to create or update. | <a href="#SingularityRequest">SingularityRequest</a> |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/request/{requestId}`

Retrieve information about a specific request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID. | string |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/requests/request/{requestId}`

Delete a specific request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to delete. | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of the person requesting the delete. | <a href="#UNKNOWN[string]">UNKNOWN[string]</a> |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


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
| user | false | Username of the person requesting the bounce | <a href="#UNKNOWN[string]">UNKNOWN[string]</a> |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/requests/request/{requestId}/run`

Schedule a Singularity request for immediate execution.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to run. | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of the person requesting the execution | <a href="#UNKNOWN[string]">UNKNOWN[string]</a> |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Additional command line arguments to append to the task | string |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/requests/request/{requestId}/pause`

Pause a Singularity request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to pause. | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of the person requesting the pause. | <a href="#UNKNOWN[string]">UNKNOWN[string]</a> |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Additional pause options. | <a href="#UNKNOWN[SingularityPauseRequest]">UNKNOWN[SingularityPauseRequest]</a> |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/requests/request/{requestId}/unpause`

Unpause a Singularity request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to unpause. | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of the person requesting the unpause | <a href="#UNKNOWN[string]">UNKNOWN[string]</a> |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/active`

Retrieve the list of active requests.


###### Parameters
- No parameters

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/paused`

Retrieve the list of paused requests.


###### Parameters
- No parameters

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/cooldown`

Retrieve the list of requests in system cooldown.


###### Parameters
- No parameters

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/finished`

Retreive the list of finished requests.


###### Parameters
- No parameters

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/queued/pending`

Retrieve the list of pending requests.


###### Parameters
- No parameters

###### Response
[](#)


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
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **PUT** `/api/requests/request/{requestId}/instances`

Scale the number of instances for a specific request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to scale. | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| user | false | Username of the person requesting the scale. | <a href="#UNKNOWN[string]">UNKNOWN[string]</a> |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Scaling information | <a href="#SingularityRequestInstances">SingularityRequestInstances</a> |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
<a name="#api-5"></a>
### /api/sandbox
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
| grep | false | Optional string to grep for | <a href="#UNKNOWN[string]">UNKNOWN[string]</a> |
| offset | false | Byte offset to start reading from | <a href="#UNKNOWN[long]">UNKNOWN[long]</a> |
| length | false | Maximum number of bytes to read | <a href="#UNKNOWN[long]">UNKNOWN[long]</a> |

###### Response
[](#)


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
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
<a name="#api-6"></a>
### /api/slaves
#### Overview
Manages Singularity slaves.

#### **GET** `/api/slaves/active`

Retrieve the list of active slaves.


###### Parameters
- No parameters

###### Response
[](#)


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
[](#)


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
[](#)


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
| user | false |  | <a href="#UNKNOWN[string]">UNKNOWN[string]</a> |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
<a name="#api-7"></a>
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
[](#)


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
[](#)


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
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
<a name="#api-8"></a>
### /api/tasks
#### Overview
Manages Singularity tasks.

#### **GET** `/api/tasks/scheduled`

Retrieve list of scheduled tasks.


###### Parameters
- No parameters

###### Response
[](#)


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
[](#)


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
[](#)


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
[](#)


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
[](#)


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
[](#)


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
[](#)


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
[](#)


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
[](#)


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
| user | false |  | <a href="#UNKNOWN[string]">UNKNOWN[string]</a> |

###### Response
[](#)


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
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
<a name="#api-9"></a>
### /api/test
#### Overview
Misc testing endpoints.

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

Make this instanceo of Singularity believe it&#39;s lost leadership.


###### Parameters
- No parameters

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
<a name="#api-10"></a>
### /api/webhooks
#### Overview
Manages Singularity webhooks.

#### **GET** `/api/webhooks`

Retrieve a list of active webhooks.


###### Parameters
- No parameters

###### Response
[](#)


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
[](#)


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
[](#)


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
[](#)


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
[](#)


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
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -

## Data Types

<a name="#model-SingularityDeploy"></a>
## SingularityDeploy

| type | required | description |
|------|----------|-------------|
| <a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a> | optional | Custom Mesos executor id. |
| <a href="#com.hubspot.mesos.Resources">com.hubspot.mesos.Resources</a> | optional | Resources required for this deploy. |
| <a href="#com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;</a> | optional | List of URIs to download before executing the deploy command. |
| <a href="#SingularityContainerInfo">SingularityContainerInfo</a> | optional | Container information for deployment into a container. |
| <a href="#com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;</a> | optional | Command arguments. |
| <a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a> | optional | The base path for the API exposed by the deploy. Used in conjunction with the Load balancer API. |
| <a href="#com.google.common.base.Optional&lt;java.util.Map&lt;java.lang.String, java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.Map&lt;java.lang.String, java.lang.String&gt;&gt;</a> | optional | Map of metadata key/value pairs associated with the deployment. |
| <a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a> | optional | Custom Mesos executor source. |
| <a href="#com.google.common.base.Optional&lt;java.lang.Long&gt;">com.google.common.base.Optional&lt;java.lang.Long&gt;</a> | optional | Health check timeout in seconds. |
| <a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a> | optional | Deployment Healthcheck URI. |
| string | required | Singularity Request Id which is associated with this deploy. |
| <a href="#com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;</a> | optional | List of load balancer groups associated with this deployment. |
| <a href="#com.google.common.base.Optional&lt;java.lang.Boolean&gt;">com.google.common.base.Optional&lt;java.lang.Boolean&gt;</a> | optional | Allows skipping of health checks when deploying. |
| <a href="#com.google.common.base.Optional&lt;java.lang.Long&gt;">com.google.common.base.Optional&lt;java.lang.Long&gt;</a> | optional | Health check interval in seconds. |
| <a href="#com.google.common.base.Optional&lt;com.hubspot.deploy.ExecutorData&gt;">com.google.common.base.Optional&lt;com.hubspot.deploy.ExecutorData&gt;</a> | optional | Executor specific information |
| <a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a> | optional | Command to execute for this deployment. |
| <a href="#com.google.common.base.Optional&lt;java.lang.Long&gt;">com.google.common.base.Optional&lt;java.lang.Long&gt;</a> | optional | Number of seconds that a service must be healthy to consider the deployment to be successful. |
| <a href="#com.google.common.base.Optional&lt;java.lang.Long&gt;">com.google.common.base.Optional&lt;java.lang.Long&gt;</a> | optional | Deploy timestamp. |
| <a href="#com.google.common.base.Optional&lt;java.util.Map&lt;java.lang.String, java.lang.Object&gt;&gt;">com.google.common.base.Optional&lt;java.util.Map&lt;java.lang.String, java.lang.Object&gt;&gt;</a> | optional | Map (Key/Value) of options for the load balancer. |
| string | optional | Custom Mesos executor |
| <a href="#com.google.common.base.Optional&lt;java.util.Map&lt;java.lang.String, java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.Map&lt;java.lang.String, java.lang.String&gt;&gt;</a> | optional | Map of environment variable definitions. |
| <a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a> | optional | Deploy version |
| <a href="#com.google.common.base.Optional&lt;java.lang.Long&gt;">com.google.common.base.Optional&lt;java.lang.Long&gt;</a> | optional | Number of seconds that singularity waits for this service to become healthy. |
| string | required | Singularity deploy id. |


<a name="#model-SingularityDeployRequest"></a>
## SingularityDeployRequest

| type | required | description |
|------|----------|-------------|
| <a href="#com.google.common.base.Optional&lt;java.lang.Boolean&gt;">com.google.common.base.Optional&lt;java.lang.Boolean&gt;</a> | optional | If deploy is successful, also unpause the request. |
| <a href="#SingularityDeploy">SingularityDeploy</a> | required | The Singularity deploy object |
| <a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a> | optional | User owning this deploy. |


<a name="#model-SingularityRequest"></a>
## SingularityRequest

| type | required | description |
|------|----------|-------------|
| <a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a> | optional | - |
| <a href="#com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;</a> | optional | - |
| <a href="#com.google.common.base.Optional&lt;java.lang.Boolean&gt;">com.google.common.base.Optional&lt;java.lang.Boolean&gt;</a> | optional | - |
| <a href="#com.google.common.base.Optional&lt;com.hubspot.singularity.SlavePlacement&gt;">com.google.common.base.Optional&lt;com.hubspot.singularity.SlavePlacement&gt;</a> | optional | - |
| <a href="#com.google.common.base.Optional&lt;java.lang.Boolean&gt;">com.google.common.base.Optional&lt;java.lang.Boolean&gt;</a> | optional | - |
| <a href="#com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;</a> | optional | - |
| <a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a> | optional | - |
| <a href="#com.google.common.base.Optional&lt;java.lang.Integer&gt;">com.google.common.base.Optional&lt;java.lang.Integer&gt;</a> | optional | - |
| <a href="#com.google.common.base.Optional&lt;java.lang.Boolean&gt;">com.google.common.base.Optional&lt;java.lang.Boolean&gt;</a> | optional | - |
| <a href="#com.google.common.base.Optional&lt;java.lang.Long&gt;">com.google.common.base.Optional&lt;java.lang.Long&gt;</a> | optional | - |
| <a href="#com.google.common.base.Optional&lt;java.lang.Integer&gt;">com.google.common.base.Optional&lt;java.lang.Integer&gt;</a> | optional | - |
| <a href="#com.google.common.base.Optional&lt;com.hubspot.singularity.ScheduleType&gt;">com.google.common.base.Optional&lt;com.hubspot.singularity.ScheduleType&gt;</a> | optional | - |
| string | optional | - |


<a name="#model-SingularityRequestInstances"></a>
## SingularityRequestInstances

| type | required | description |
|------|----------|-------------|
| <a href="#com.google.common.base.Optional&lt;java.lang.Integer&gt;">com.google.common.base.Optional&lt;java.lang.Integer&gt;</a> | optional | - |
| string | optional | - |


<a name="#model-SingularityWebhook"></a>
## SingularityWebhook

| type | required | description |
|------|----------|-------------|
| <a href="#WebhookType">WebhookType</a> | optional | Webhook type (TASK, REQUEST, DEPLOY). Allowable
        values:TASK, REQUEST, DEPLOY |
| string | optional | URI to POST to. |
| <a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a> | optional | User that created webhook. |
| long | optional | - |
| string | optional | Unique ID for webhook. |


