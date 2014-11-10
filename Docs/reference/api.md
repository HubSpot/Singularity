# Singularity REST API

Version: 0.4.0-SNAPSHOT

Endpoints:
- `/api/deploys` - Manages Singularity deploys for existing requests.
- `/api/history` - Manages historical data for tasks, requests, and deploys.
- `/api/logs` - Manages Singularity task logs stored in S3.
- `/api/racks` - Manages Singularity racks.
- `/api/requests` - Manages Singularity requests.
- `/api/sandbox` - Provides a proxy to Mesos sandboxes.
- `/api/slaves` - Manages Singularity slaves.
- `/api/state` - Provides information about the current state of Singularity.
- `/api/tasks` - Manages Singularity tasks.
- `/api/test` - Misc testing endpoints.
- `/api/webhooks` - Manages Singularity webhooks.

Models:
- `EmbeddedArtifact`
- `ExecutorData`
- `ExternalArtifact`
- `S3Artifact`
- `SingularityContainerInfo`
- `SingularityDeploy`
- `SingularityDeployRequest`
- `SingularityDockerInfo`
- `SingularityDockerPortMapping`
- `SingularityPauseRequest`
- `SingularityRequest`
- `SingularityRequestInstances`
- `SingularityVolume`
- `SingularityWebhook`

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
| user | false | The user which executes the delete request. | string |

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Deploy is not pending or not present. | - |
| 200    | Pending deploy successfully removed. | - |


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
<a name="#api-1"></a>
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
<a name="#api-2"></a>
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
<a name="#api-3"></a>
### /api/racks
#### Overview
Manages Singularity racks.

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
| user | false | Username of the person requesting to create or update. | string |
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
| user | false | Username of the person requesting the delete. | string |

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
| user | false | Username of the person requesting the bounce | string |

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
| user | false | Username of the person requesting the scale. | string |
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
| user | false | Username of the person requesting the pause. | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Additional pause options. | <a href="#SingularityPauseRequest">SingularityPauseRequest</a> |

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
| user | false | Username of the person requesting the execution | string |
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
| user | false | Username of the person requesting the unpause | string |

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
<a name="#api-8"></a>
### /api/tasks
#### Overview
Manages Singularity tasks.

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
| user | false |  | string |

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

## Data Types

<a name="#model-EmbeddedArtifact"></a>
## EmbeddedArtifact

| name | type | required | description |
|------|------|----------|-------------|
| md5sum | string | optional |  |
| filename | string | optional |  |
| name | string | optional |  |
| content | <a href="#byte">Array[byte]</a> | optional |  |


<a name="#model-ExecutorData"></a>
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


<a name="#model-ExternalArtifact"></a>
## ExternalArtifact

| name | type | required | description |
|------|------|----------|-------------|
| md5sum | string | optional |  |
| url | string | optional |  |
| filename | string | optional |  |
| filesize | long | optional |  |
| name | string | optional |  |


<a name="#model-S3Artifact"></a>
## S3Artifact

| name | type | required | description |
|------|------|----------|-------------|
| s3Bucket | string | optional |  |
| md5sum | string | optional |  |
| filename | string | optional |  |
| filesize | long | optional |  |
| s3ObjectKey | string | optional |  |
| name | string | optional |  |


<a name="#model-SingularityContainerInfo"></a>
## SingularityContainerInfo

| name | type | required | description |
|------|------|----------|-------------|
| type | <a href="#Type">Type</a> | optional |  Allowable values: DOCKER |
| volumes | <a href="#SingularityVolume">Array[SingularityVolume]</a> | optional |  |
| docker | <a href="#SingularityDockerInfo">SingularityDockerInfo</a> | optional |  |


<a name="#model-SingularityDeploy"></a>
## SingularityDeploy

| name | type | required | description |
|------|------|----------|-------------|
| customExecutorId | string | optional | Custom Mesos executor id. |
| resources | <a href="#com.hubspot.mesos.Resources">com.hubspot.mesos.Resources</a> | optional | Resources required for this deploy. |
| uris | Array[string] | optional | List of URIs to download before executing the deploy command. |
| containerInfo | <a href="#SingularityContainerInfo">SingularityContainerInfo</a> | optional | Container information for deployment into a container. |
| arguments | Array[string] | optional | Command arguments. |
| serviceBasePath | string | optional | The base path for the API exposed by the deploy. Used in conjunction with the Load balancer API. |
| metadata | <a href="#Map[string,string]">Map[string,string]</a> | optional | Map of metadata key/value pairs associated with the deployment. |
| customExecutorSource | string | optional | Custom Mesos executor source. |
| healthcheckTimeoutSeconds | long | optional | Health check timeout in seconds. |
| healthcheckUri | string | optional | Deployment Healthcheck URI. |
| requestId | string | required | Singularity Request Id which is associated with this deploy. |
| loadBalancerGroups | Array[string] | optional | List of load balancer groups associated with this deployment. |
| skipHealthchecksOnDeploy | boolean | optional | Allows skipping of health checks when deploying. |
| healthcheckIntervalSeconds | long | optional | Health check interval in seconds. |
| executorData | <a href="#ExecutorData">ExecutorData</a> | optional | Executor specific information |
| command | string | optional | Command to execute for this deployment. |
| considerHealthyAfterRunningForSeconds | long | optional | Number of seconds that a service must be healthy to consider the deployment to be successful. |
| timestamp | long | optional | Deploy timestamp. |
| loadBalancerOptions | <a href="#Map[string,Object]">Map[string,Object]</a> | optional | Map (Key/Value) of options for the load balancer. |
| customExecutorCmd | string | optional | Custom Mesos executor |
| env | <a href="#Map[string,string]">Map[string,string]</a> | optional | Map of environment variable definitions. |
| version | string | optional | Deploy version |
| deployHealthTimeoutSeconds | long | optional | Number of seconds that singularity waits for this service to become healthy. |
| id | string | required | Singularity deploy id. |


<a name="#model-SingularityDeployRequest"></a>
## SingularityDeployRequest

| name | type | required | description |
|------|------|----------|-------------|
| unpauseOnSuccessfulDeploy | boolean | optional | If deploy is successful, also unpause the request. |
| deploy | <a href="#SingularityDeploy">SingularityDeploy</a> | required | The Singularity deploy object |
| user | string | optional | User owning this deploy. |


<a name="#model-SingularityDockerInfo"></a>
## SingularityDockerInfo

| name | type | required | description |
|------|------|----------|-------------|
| network | <a href="#Network">Network</a> | optional |  |
| portMappings | <a href="#SingularityDockerPortMapping">Array[SingularityDockerPortMapping]</a> | optional |  |
| image | string | optional |  |


<a name="#model-SingularityDockerPortMapping"></a>
## SingularityDockerPortMapping

| name | type | required | description |
|------|------|----------|-------------|
| hostPort | int | optional |  |
| containerPort | int | optional |  |
| containerPortType | <a href="#SingularityPortMappingType">SingularityPortMappingType</a> | optional |  Allowable values: LITERAL, FROM_OFFER |
| protocol | string | optional |  |
| hostPortType | <a href="#SingularityPortMappingType">SingularityPortMappingType</a> | optional |  Allowable values: LITERAL, FROM_OFFER |


<a name="#model-SingularityPauseRequest"></a>
## SingularityPauseRequest

| name | type | required | description |
|------|------|----------|-------------|
| user | string | optional |  |
| killTasks | boolean | optional |  |


<a name="#model-SingularityRequest"></a>
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
| numRetriesOnFailure | int | optional |  |
| loadBalanced | boolean | optional |  |
| killOldNonLongRunningTasksAfterMillis | long | optional |  |
| instances | int | optional |  |
| scheduleType | <a href="#ScheduleType">ScheduleType</a> | optional |  |
| id | string | optional |  |


<a name="#model-SingularityRequestInstances"></a>
## SingularityRequestInstances

| name | type | required | description |
|------|------|----------|-------------|
| instances | int | optional |  |
| id | string | optional |  |


<a name="#model-SingularityVolume"></a>
## SingularityVolume

| name | type | required | description |
|------|------|----------|-------------|
| hostPath | string | optional |  |
| containerPath | string | optional |  |
| mode | <a href="#Mode">Mode</a> | optional |  Allowable values: RW, RO |


<a name="#model-SingularityWebhook"></a>
## SingularityWebhook

| name | type | required | description |
|------|------|----------|-------------|
| type | <a href="#WebhookType">WebhookType</a> | optional | Webhook type. Allowable values: TASK, REQUEST, DEPLOY |
| uri | string | optional | URI to POST to. |
| user | string | optional | User that created webhook. |
| timestamp | long | optional |  |
| id | string | optional | Unique ID for webhook. |


