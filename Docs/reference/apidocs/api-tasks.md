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
[MesosTaskStatisticsObject](models.md#model-MesosTaskStatisticsObject)


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
| body | false |  | [SingularityTaskMetadataRequest](models.md#model-linkType)</a> |

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
| body | false |  | [SingularityShellCommand](models.md#model-linkType)</a> |

###### Response
[SingularityTaskShellCommandRequest](models.md#model-SingularityTaskShellCommandRequest)


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
[SingularityTaskCleanup](models.md#model-SingularityTaskCleanup)


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
| body | false |  | [SingularityKillTaskRequest](models.md#model-linkType)</a> |

###### Response
[SingularityTaskCleanup](models.md#model-SingularityTaskCleanup)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 409    | Task already has a cleanup request (can be overridden with override=true) | - |


- - -
#### **GET** `/api/tasks/task/{taskId}`

Retrieve information about a specific active task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |

###### Response
[SingularityTask](models.md#model-SingularityTask)


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
[SingularityTaskRequest](models.md#model-SingularityTaskRequest)


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

###### Response
[List[SingularityTaskRequest]](models.md#model-SingularityTaskRequest)


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

###### Response
[UNKNOWN[SingularityPendingTaskId]](models.md#model-UNKNOWN[SingularityPendingTaskId])


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

###### Response
[List[SingularityTaskRequest]](models.md#model-SingularityTaskRequest)


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
[UNKNOWN[SingularityTaskId]](models.md#model-UNKNOWN[SingularityTaskId])


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
[UNKNOWN[SingularityKilledTaskIdRecord]](models.md#model-UNKNOWN[SingularityKilledTaskIdRecord])


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

###### Response
[UNKNOWN[SingularityTaskCleanup]](models.md#model-UNKNOWN[SingularityTaskCleanup])


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

###### Response
[UNKNOWN[SingularityTask]](models.md#model-UNKNOWN[SingularityTask])


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

###### Response
[UNKNOWN[SingularityTask]](models.md#model-UNKNOWN[SingularityTask])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -