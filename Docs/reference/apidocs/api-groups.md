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
[SingularityRequestGroup](models.md#model-SingularityRequestGroup)


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
[List[SingularityRequestGroup]](models.md#model-SingularityRequestGroup)


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
| body | false |  | [SingularityRequestGroup](models.md#model-linkType)</a> |

###### Response
[SingularityRequestGroup](models.md#model-SingularityRequestGroup)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -