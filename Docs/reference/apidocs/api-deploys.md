#### Overview
Manages Singularity Deploys for existing requests

#### **POST** `/api/deploys/update`

Update the target active instance count for a pending deploy


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | true |  | [SingularityUpdatePendingDeployRequest](models.md#model-linkType)</a> |

###### Response
[SingularityRequestParent](models.md#model-SingularityRequestParent)


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
[List[SingularityPendingDeploy]](models.md#model-SingularityPendingDeploy)


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
[SingularityRequestParent](models.md#model-SingularityRequestParent)


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
| body | true |  | [SingularityDeployRequest](models.md#model-linkType)</a> |

###### Response
[SingularityRequestParent](models.md#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Deploy object is invalid | - |
| 409    | A current deploy is in progress. It may be canceled by calling DELETE | - |


- - -