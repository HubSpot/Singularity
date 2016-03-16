#### Overview
Manages Singularity Requests, the parent object for any deployed task

#### **POST** `/api/requests/request/{requestId}/unpause`

Unpause a Singularity Request, scheduling new tasks immediately


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to unpause | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityUnpauseRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 409    | Request is not paused | - |


- - -
#### **PUT** `/api/requests/request/{requestId}/skipHealthchecks`

Update the skipHealthchecks field for the request, possibly temporarily


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID to scale | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | SkipHealtchecks options | [SingularitySkipHealthchecksRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request with that ID | - |


- - -
#### **DELETE** `/api/requests/request/{requestId}/skipHealthchecks`

Delete/cancel the expiring skipHealthchecks. This makes the skipHealthchecks request permanent.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID | string |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request or expiring skipHealthchecks request for that ID | - |


- - -
#### **PUT** `/api/requests/request/{requestId}/scale`

Scale the number of instances up or down for a specific Request


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID to scale | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Object to hold number of instances to request | [SingularityScaleRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request with that ID | - |


- - -
#### **DELETE** `/api/requests/request/{requestId}/scale`

Delete/cancel the expiring scale. This makes the scale request permanent.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID | string |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request or expiring scale request for that ID | - |


- - -
#### **POST** `/api/requests/request/{requestId}/run`

Schedule a one-off or scheduled Singularity request for immediate execution.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to run | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityRunNowRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Singularity Request is not scheduled or one-off | - |


- - -
#### **POST** `/api/requests/request/{requestId}/pause`

Pause a Singularity request, future tasks will not run until it is manually unpaused. API can optionally choose to kill existing tasks


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to pause | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Pause Request Options | [SingularityPauseRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 409    | Request is already paused or being cleaned | - |


- - -
#### **DELETE** `/api/requests/request/{requestId}/pause`

Delete/cancel the expiring pause. This makes the pause request permanent.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID | string |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request or expiring pause request for that ID | - |


- - -
#### **POST** `/api/requests/request/{requestId}/exit-cooldown`

Immediately exits cooldown, scheduling new tasks immediately


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true |  | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityExitCooldownRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 409    | Request is not in cooldown | - |


- - -
#### **POST** `/api/requests/request/{requestId}/bounce`

Bounce a specific Singularity request. A bounce launches replacement task(s), and then kills the original task(s) if the replacement(s) are healthy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to bounce | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Bounce request options | [SingularityBounceRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/requests/request/{requestId}/bounce`

Delete/cancel the expiring bounce. This makes the bounce request permanent.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The Request ID | string |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request or expiring bounce request for that ID | - |


- - -
#### **GET** `/api/requests/request/{requestId}`

Retrieve a specific Request by ID


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID | string |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


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
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | Delete options | [SingularityDeleteRequestRequest](#model-linkType)</a> |

###### Response
[SingularityRequest](#model-SingularityRequest)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | No Request with that ID | - |


- - -
#### **GET** `/api/requests/queued/pending`

Retrieve the list of pending requests


###### Parameters
- No parameters

###### Response
[List[SingularityPendingRequest]](#model-SingularityPendingRequest)


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
[List[SingularityRequestCleanup]](#model-SingularityRequestCleanup)


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
[List[SingularityRequestParent]](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/lbcleanup`

Retrieve the list of tasks being cleaned from load balancers.


###### Parameters
- No parameters

###### Response
[UNKNOWN[string]](#model-UNKNOWN[string])


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
[List[SingularityRequestParent]](#model-SingularityRequestParent)


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
[List[SingularityRequestParent]](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests/active`

Retrieve the list of active requests


###### Parameters
- No parameters

###### Response
[List[SingularityRequestParent]](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests`

Retrieve the list of all requests


###### Parameters
- No parameters

###### Response
[List[SingularityRequestParent]](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/requests`

Create or update a Singularity Request


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false | The Singularity request to create or update | [SingularityRequest](#model-linkType)</a> |

###### Response
[SingularityRequestParent](#model-SingularityRequestParent)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Request object is invalid | - |
| 409    | Request object is being cleaned. Try again shortly | - |


- - -