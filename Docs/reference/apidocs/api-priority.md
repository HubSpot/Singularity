#### Overview
Manages whether or not to schedule tasks based on their priority levels.

#### **GET** `/api/priority/freeze`

Get information about the active priority freeze.


###### Parameters
- No parameters

###### Response
[SingularityPriorityFreezeParent](models.md#model-SingularityPriorityFreezeParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 200    | The active priority freeze. | - |
| 404    | There was no active priority freeze. | - |


- - -
#### **DELETE** `/api/priority/freeze`

Stops the active priority freeze.


###### Parameters
- No parameters

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
| body | false |  | [SingularityPriorityFreeze](models.md#model-linkType)</a> |

###### Response
[SingularityPriorityFreezeParent](models.md#model-SingularityPriorityFreezeParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 200    | The priority freeze request was accepted. | - |
| 400    | There was a validation error with the priority freeze request. | - |


- - -