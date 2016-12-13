#### Overview
Manages historical data for tasks, requests, and deploys.

#### **GET** `/api/history/tasks/withmetadata`

Retrieve the history sorted by startedAt for all inactive tasks.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | false | Optional Request ID to match | string |
| deployId | false | Optional deploy ID to match | string |
| runId | false | Optional runId to match | string |
| host | false | Optional host to match | string |
| lastTaskStatus | false | Optional last task status to match | string |
| startedBefore | false | Optionally match only tasks started before | long |
| startedAfter | false | Optionally match only tasks started after | long |
| updatedBefore | false | Optionally match tasks last updated before | long |
| updatedAfter | false | Optionally match tasks last updated after | long |
| orderDirection | false | Sort direction | string |
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |

###### Response
[UNKNOWN[SingularityTaskIdHistory]](models.md#model-UNKNOWN[SingularityTaskIdHistory])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/tasks`

Retrieve the history sorted by startedAt for all inactive tasks.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | false | Optional Request ID to match | string |
| deployId | false | Optional deploy ID to match | string |
| runId | false | Optional runId to match | string |
| host | false | Optional host to match | string |
| lastTaskStatus | false | Optional last task status to match | string |
| startedBefore | false | Optionally match only tasks started before | long |
| startedAfter | false | Optionally match only tasks started after | long |
| updatedBefore | false | Optionally match tasks last updated before | long |
| updatedAfter | false | Optionally match tasks last updated after | long |
| orderDirection | false | Sort direction | string |
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |

###### Response
[List[SingularityTaskIdHistory]](models.md#model-SingularityTaskIdHistory)


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
[SingularityTaskHistory](models.md#model-SingularityTaskHistory)


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
[UNKNOWN[string]](models.md#model-UNKNOWN[string])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/tasks/withmetadata`

Retrieve the history count for all inactive tasks of a specific request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to match | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| deployId | false | Optional deploy ID to match | string |
| runId | false | Optional runId to match | string |
| host | false | Optional host to match | string |
| lastTaskStatus | false | Optional last task status to match | string |
| startedBefore | false | Optionally match only tasks started before | long |
| startedAfter | false | Optionally match only tasks started after | long |
| updatedBefore | false | Optionally match tasks last updated before | long |
| updatedAfter | false | Optionally match tasks last updated after | long |
| orderDirection | false | Sort direction | string |
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |

###### Response
[UNKNOWN[SingularityTaskIdHistory]](models.md#model-UNKNOWN[SingularityTaskIdHistory])


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
[List[SingularityTaskIdHistory]](models.md#model-SingularityTaskIdHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/tasks`

Retrieve the history sorted by startedAt for all inactive tasks of a specific request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to match | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| deployId | false | Optional deploy ID to match | string |
| runId | false | Optional runId to match | string |
| host | false | Optional host to match | string |
| lastTaskStatus | false | Optional last task status to match | string |
| startedBefore | false | Optionally match only tasks started before | long |
| startedAfter | false | Optionally match only tasks started after | long |
| updatedBefore | false | Optionally match tasks last updated before | long |
| updatedAfter | false | Optionally match tasks last updated after | long |
| orderDirection | false | Sort direction | string |
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |

###### Response
[List[SingularityTaskIdHistory]](models.md#model-SingularityTaskIdHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/run/{runId}`

Retrieve the history for a task by runId


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to look up | string |
| runId | true | runId to look up | string |

###### Response
[SingularityTaskIdHistory](models.md#model-SingularityTaskIdHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/requests/withmetadata`

Get request history for a single request


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
[UNKNOWN[SingularityRequestHistory]](models.md#model-UNKNOWN[SingularityRequestHistory])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/requests`

Get request history for a single request


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
[List[SingularityRequestHistory]](models.md#model-SingularityRequestHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploys/withmetadata`

Get deploy history with metadata for a single request


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
[UNKNOWN[SingularityDeployHistory]](models.md#model-UNKNOWN[SingularityDeployHistory])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploys`

Get deploy history for a single request


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
[List[SingularityDeployHistory]](models.md#model-SingularityDeployHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploy/{deployId}/tasks/inactive/withmetadata`

Retrieve the task history for a specific deploy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID for deploy | string |
| deployId | true | Deploy ID | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |

###### Response
[UNKNOWN[SingularityTaskIdHistory]](models.md#model-UNKNOWN[SingularityTaskIdHistory])


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploy/{deployId}/tasks/inactive`

Retrieve the task history for a specific deploy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID for deploy | string |
| deployId | true | Deploy ID | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| count | false | Maximum number of items to return | int |
| page | false | Which page of items to view | int |

###### Response
[List[SingularityTaskIdHistory]](models.md#model-SingularityTaskIdHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploy/{deployId}/tasks/active`

Retrieve the task history for a specific deploy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID for deploy | string |
| deployId | true | Deploy ID | string |

###### Response
[List[SingularityTaskIdHistory]](models.md#model-SingularityTaskIdHistory)


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
[SingularityDeployHistory](models.md#model-SingularityDeployHistory)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/command-line-args`

Get a list of recently used command line args for an on-demand or scheduled request


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | Request ID to look up | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| count | false | Max number of recent args to return | int |

###### Response
[Set](models.md#model-Set)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -