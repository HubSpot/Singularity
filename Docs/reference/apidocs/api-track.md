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
| body | false |  | [SingularityUser](models.md#model-linkType)</a> |

###### Response
[SingularityTaskState](models.md#model-SingularityTaskState)


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
| body | false |  | [SingularityUser](models.md#model-linkType)</a> |

###### Response
[SingularityTaskState](models.md#model-SingularityTaskState)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | Task with this runId does not exist | - |


- - -