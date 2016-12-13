#### Overview
Manages Singularity Deploys for existing requests

#### **GET** `/api/disasters/stats`

Get current data related to disaster detection


###### Parameters
- No parameters

###### Response
[SingularityDisastersData](models.md#model-SingularityDisastersData)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/disasters/enable`

Allow the automated poller to disable actions when a disaster is detected


###### Parameters
- No parameters

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
| body | false |  | [SingularityDisabledActionRequest](models.md#model-linkType)</a> |

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

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/disasters/disabled-actions`

Get a list of actions that are currently disable


###### Parameters
- No parameters

###### Response
[List[SingularityDisabledAction]](models.md#model-SingularityDisabledAction)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/disasters/disable`

Do not allow the automated poller to disable actions when a disaster is detected


###### Parameters
- No parameters

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

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/disasters/active`

Get a list of current active disasters


###### Parameters
- No parameters

###### Response
List[string]


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -