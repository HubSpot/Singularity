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
[List[SingularityTaskHistoryUpdate]](models.md#model-SingularityTaskHistoryUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks/task`

Retrieve a list of queued task updates for a specific webhook.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | false |  | string |

###### Response
[List[SingularityTaskHistoryUpdate]](models.md#model-SingularityTaskHistoryUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks/summary`

Retrieve a summary of each active webhook


###### Parameters
- No parameters

###### Response
[List[SingularityWebhookSummary]](models.md#model-SingularityWebhookSummary)


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
[List[SingularityRequestHistory]](models.md#model-SingularityRequestHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks/request`

Retrieve a list of queued request updates for a specific webhook.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | false |  | string |

###### Response
[List[SingularityRequestHistory]](models.md#model-SingularityRequestHistory)


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
[List[SingularityDeployUpdate]](models.md#model-SingularityDeployUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/webhooks/deploy`

Retrieve a list of queued deploy updates for a specific webhook.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | false |  | string |

###### Response
[List[SingularityDeployUpdate]](models.md#model-SingularityDeployUpdate)


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
[List[SingularityWebhook]](models.md#model-SingularityWebhook)


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
| body | false |  | [SingularityWebhook](models.md#model-linkType)</a> |

###### Response
string


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/webhooks`

Delete a specific webhook.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| webhookId | false |  | string |

###### Response
string


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -