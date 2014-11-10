#Singularity REST API
Singularity Mesos Framework API



Api Version: 0.4.0-SNAPSHOT

## Endpoints
### /api/deploys

#### **POST** `/api/deploys`

Create a new deployment.


###### Parameters
**body**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>true</td>
        <td></td>
        <td><a href="#SingularityDeployRequest">SingularityDeployRequest</a></td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 200    | Deploy successfully scheduled. | - |
| 400    | Deploy object is invalid. | - |
| 409    | A current deploy is in progress. | - |


- - -
#### **GET** `/api/deploys/pending`

Retrieve the list of pending deploys.


###### Parameters

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/deploys/deploy/{deployId}/request/{requestId}`

Delete a pending deployment from a request.


###### Parameters
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>user</th>
        <td>false</td>
        <td>The user which executes the delete request.</td>
        <td><a href="#UNKNOWN[string]">UNKNOWN[string]</a></td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>The Singularity Request Id from which the deployment is removed.</td>
        <td>string</td>
    </tr>
    <tr>
        <th>deployId</th>
        <td>true</td>
        <td>The Singularity Deploy Id that should be removed.</td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Deploy is not pending or not present. | - |
| 200    | Pending deploy successfully removed. | - |


- - -
### /api/history

#### **GET** `/api/history/task/{taskId}`

Retrieve the history for a specific task.


###### Parameters
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>taskId</th>
        <td>true</td>
        <td>Task ID to look up</td>
        <td>string</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>Request ID to look up</td>
        <td>string</td>
    </tr>
</table>

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
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>count</th>
        <td>false</td>
        <td>Maximum number of items to return</td>
        <td>int</td>
    </tr>
    <tr>
        <th>page</th>
        <td>false</td>
        <td>Which page of items to view</td>
        <td>int</td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>Request ID to look up</td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploy/{deployId}`

Retrieve the history for a specific deploy.


###### Parameters
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>Request ID for deploy</td>
        <td>string</td>
    </tr>
    <tr>
        <th>deployId</th>
        <td>true</td>
        <td>Deploy ID</td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/deploys`




###### Parameters
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>count</th>
        <td>false</td>
        <td>Maximum number of items to return</td>
        <td>int</td>
    </tr>
    <tr>
        <th>page</th>
        <td>false</td>
        <td>Which page of items to view</td>
        <td>int</td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>Request ID to look up</td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/history/request/{requestId}/requests`




###### Parameters
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>count</th>
        <td>false</td>
        <td>Naximum number of items to return</td>
        <td>int</td>
    </tr>
    <tr>
        <th>page</th>
        <td>false</td>
        <td>Which page of items to view</td>
        <td>int</td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>Request ID to look up</td>
        <td>string</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestIdLike</th>
        <td>false</td>
        <td>Request ID prefix to search for</td>
        <td>string</td>
    </tr>
    <tr>
        <th>count</th>
        <td>false</td>
        <td>Maximum number of items to return</td>
        <td>int</td>
    </tr>
    <tr>
        <th>page</th>
        <td>false</td>
        <td>Which page of items to view</td>
        <td>int</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/logs

#### **GET** `/api/logs/task/{taskId}`

Retrieve the list of logs stored in S3 for a specific task.


###### Parameters
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>taskId</th>
        <td>true</td>
        <td>The task ID to search for</td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/logs/request/{requestId}`

Retrieve the list of logs stored in S3 for a specific request.


###### Parameters
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>The request ID to search for</td>
        <td>string</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>The request ID to search for</td>
        <td>string</td>
    </tr>
    <tr>
        <th>deployId</th>
        <td>true</td>
        <td>The deploy ID to search for</td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/racks

#### **GET** `/api/racks/active`

Retrieve the list of active racks. A rack is active if it has one or more active slaves associated with it.


###### Parameters

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>rackId</th>
        <td>true</td>
        <td>Dead rack ID.</td>
        <td>string</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>rackId</th>
        <td>true</td>
        <td>Decommissioned rack ID.</td>
        <td>string</td>
    </tr>
</table>

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
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>user</th>
        <td>false</td>
        <td>Username of person requestin the decommisioning.</td>
        <td><a href="#UNKNOWN[string]">UNKNOWN[string]</a></td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>rackId</th>
        <td>true</td>
        <td>Active rack ID.</td>
        <td>string</td>
    </tr>
</table>

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

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/requests

#### **POST** `/api/requests`

Create or update a Singularity Request


###### Parameters
**body**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td>The Singularity request to create or update.</td>
        <td><a href="#SingularityRequest">SingularityRequest</a></td>
    </tr>
</table>
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>user</th>
        <td>false</td>
        <td>Username of the person requesting to create or update.</td>
        <td><a href="#UNKNOWN[string]">UNKNOWN[string]</a></td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/requests`

Retrieve the list of all requests.


###### Parameters

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>Request ID.</td>
        <td>string</td>
    </tr>
</table>

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
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>user</th>
        <td>false</td>
        <td>Username of the person requesting the delete.</td>
        <td><a href="#UNKNOWN[string]">UNKNOWN[string]</a></td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>The request ID to delete.</td>
        <td>string</td>
    </tr>
</table>

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
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>user</th>
        <td>false</td>
        <td>Username of the person requesting the bounce</td>
        <td><a href="#UNKNOWN[string]">UNKNOWN[string]</a></td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>The request ID to bounce</td>
        <td>string</td>
    </tr>
</table>

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
**body**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td>Additional command line arguments to append to the task</td>
        <td>string</td>
    </tr>
</table>
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>user</th>
        <td>false</td>
        <td>Username of the person requesting the execution</td>
        <td><a href="#UNKNOWN[string]">UNKNOWN[string]</a></td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>The request ID to run.</td>
        <td>string</td>
    </tr>
</table>

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
**body**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td>Additional pause options.</td>
        <td><a href="#UNKNOWN[SingularityPauseRequest]">UNKNOWN[SingularityPauseRequest]</a></td>
    </tr>
</table>
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>user</th>
        <td>false</td>
        <td>Username of the person requesting the pause.</td>
        <td><a href="#UNKNOWN[string]">UNKNOWN[string]</a></td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>The request ID to pause.</td>
        <td>string</td>
    </tr>
</table>

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
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>user</th>
        <td>false</td>
        <td>Username of the person requesting the unpause</td>
        <td><a href="#UNKNOWN[string]">UNKNOWN[string]</a></td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>The request ID to unpause.</td>
        <td>string</td>
    </tr>
</table>

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
**body**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td>Scaling information</td>
        <td><a href="#SingularityRequestInstances">SingularityRequestInstances</a></td>
    </tr>
</table>
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>user</th>
        <td>false</td>
        <td>Username of the person requesting the scale.</td>
        <td><a href="#UNKNOWN[string]">UNKNOWN[string]</a></td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td>The request ID to scale.</td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/sandbox

#### **GET** `/api/sandbox/{taskId}/read`

Retrieve part of the contents of a file in a specific task&#39;s sandbox.


###### Parameters
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>path</th>
        <td>false</td>
        <td>The path to the file to be read</td>
        <td>string</td>
    </tr>
    <tr>
        <th>grep</th>
        <td>false</td>
        <td>Optional string to grep for</td>
        <td><a href="#UNKNOWN[string]">UNKNOWN[string]</a></td>
    </tr>
    <tr>
        <th>offset</th>
        <td>false</td>
        <td>Byte offset to start reading from</td>
        <td><a href="#UNKNOWN[long]">UNKNOWN[long]</a></td>
    </tr>
    <tr>
        <th>length</th>
        <td>false</td>
        <td>Maximum number of bytes to read</td>
        <td><a href="#UNKNOWN[long]">UNKNOWN[long]</a></td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>taskId</th>
        <td>true</td>
        <td>The task ID of the sandbox to read from</td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/sandbox/{taskId}/browse`

Retrieve information about a specific task&#39;s sandbox.


###### Parameters
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>path</th>
        <td>false</td>
        <td>The path to browse from</td>
        <td>string</td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>taskId</th>
        <td>true</td>
        <td>The task ID to browse</td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/slaves

#### **GET** `/api/slaves/dead`

Retrieve the list of dead slaves.


###### Parameters

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

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/slaves/active`

Retrieve the list of active slaves.


###### Parameters

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>slaveId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>slaveId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

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
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>user</th>
        <td>false</td>
        <td></td>
        <td><a href="#UNKNOWN[string]">UNKNOWN[string]</a></td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>slaveId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/state

#### **GET** `/api/state`

Retrieve information about the current state of Singularity.


###### Parameters
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>skipCache</th>
        <td>false</td>
        <td></td>
        <td>boolean</td>
    </tr>
    <tr>
        <th>includeRequestIds</th>
        <td>false</td>
        <td></td>
        <td>boolean</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>skipCache</th>
        <td>false</td>
        <td></td>
        <td>boolean</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>skipCache</th>
        <td>false</td>
        <td></td>
        <td>boolean</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/tasks

#### **GET** `/api/tasks/scheduled`

Retrieve list of scheduled tasks.


###### Parameters

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>pendingTaskId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>requestId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>slaveId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/tasks/active`

Retrieve the list of active tasks.


###### Parameters

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>taskId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

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
**query**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>user</th>
        <td>false</td>
        <td></td>
        <td><a href="#UNKNOWN[string]">UNKNOWN[string]</a></td>
    </tr>
</table>
**path**

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>taskId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>taskId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/test

#### **POST** `/api/test/abort`

Abort the Mesos scheduler driver.


###### Parameters

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>taskId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
    <tr>
        <th>taskState</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

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

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
### /api/webhooks

#### **GET** `/api/webhooks`

Retrieve a list of active webhooks.


###### Parameters

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#SingularityWebhook">SingularityWebhook</a></td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>webhookId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>webhookId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>webhookId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

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

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>webhookId</th>
        <td>true</td>
        <td></td>
        <td>string</td>
    </tr>
</table>

###### Response
[](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -

## Data Types


## <a name="SingularityDeploy">SingularityDeploy</a>

<table border="1">
    <tr>
        <th>type</th>
        <th>required</th>
        <th>description</th>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a></td>
        <td>optional</td>
        <td>Custom Mesos executor id.</td>
    </tr>
    <tr>
        <td><a href="#com.hubspot.mesos.Resources">com.hubspot.mesos.Resources</a></td>
        <td>optional</td>
        <td>Resources required for this deploy.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;</a></td>
        <td>optional</td>
        <td>List of URIs to download before executing the deploy command.</td>
    </tr>
    <tr>
        <td><a href="#SingularityContainerInfo">SingularityContainerInfo</a></td>
        <td>optional</td>
        <td>Container information for deployment into a container.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;</a></td>
        <td>optional</td>
        <td>Command arguments.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a></td>
        <td>optional</td>
        <td>The base path for the API exposed by the deploy. Used in conjunction with the Load balancer API.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.util.Map&lt;java.lang.String, java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.Map&lt;java.lang.String, java.lang.String&gt;&gt;</a></td>
        <td>optional</td>
        <td>Map of metadata key/value pairs associated with the deployment.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a></td>
        <td>optional</td>
        <td>Custom Mesos executor source.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Long&gt;">com.google.common.base.Optional&lt;java.lang.Long&gt;</a></td>
        <td>optional</td>
        <td>Health check timeout in seconds.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a></td>
        <td>optional</td>
        <td>Deployment Healthcheck URI.</td>
    </tr>
    <tr>
        <td>string</td>
        <td>required</td>
        <td>Singularity Request Id which is associated with this deploy.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;</a></td>
        <td>optional</td>
        <td>List of load balancer groups associated with this deployment.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Boolean&gt;">com.google.common.base.Optional&lt;java.lang.Boolean&gt;</a></td>
        <td>optional</td>
        <td>Allows skipping of health checks when deploying.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Long&gt;">com.google.common.base.Optional&lt;java.lang.Long&gt;</a></td>
        <td>optional</td>
        <td>Health check interval in seconds.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;com.hubspot.deploy.ExecutorData&gt;">com.google.common.base.Optional&lt;com.hubspot.deploy.ExecutorData&gt;</a></td>
        <td>optional</td>
        <td>Executor specific information</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a></td>
        <td>optional</td>
        <td>Command to execute for this deployment.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Long&gt;">com.google.common.base.Optional&lt;java.lang.Long&gt;</a></td>
        <td>optional</td>
        <td>Number of seconds that a service must be healthy to consider the deployment to be successful.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Long&gt;">com.google.common.base.Optional&lt;java.lang.Long&gt;</a></td>
        <td>optional</td>
        <td>Deploy timestamp.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.util.Map&lt;java.lang.String, java.lang.Object&gt;&gt;">com.google.common.base.Optional&lt;java.util.Map&lt;java.lang.String, java.lang.Object&gt;&gt;</a></td>
        <td>optional</td>
        <td>Map (Key/Value) of options for the load balancer.</td>
    </tr>
    <tr>
        <td>string</td>
        <td>optional</td>
        <td>Custom Mesos executor</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.util.Map&lt;java.lang.String, java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.Map&lt;java.lang.String, java.lang.String&gt;&gt;</a></td>
        <td>optional</td>
        <td>Map of environment variable definitions.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a></td>
        <td>optional</td>
        <td>Deploy version</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Long&gt;">com.google.common.base.Optional&lt;java.lang.Long&gt;</a></td>
        <td>optional</td>
        <td>Number of seconds that singularity waits for this service to become healthy.</td>
    </tr>
    <tr>
        <td>string</td>
        <td>required</td>
        <td>Singularity deploy id.</td>
    </tr>
</table>



## <a name="SingularityDeployRequest">SingularityDeployRequest</a>

<table border="1">
    <tr>
        <th>type</th>
        <th>required</th>
        <th>description</th>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Boolean&gt;">com.google.common.base.Optional&lt;java.lang.Boolean&gt;</a></td>
        <td>optional</td>
        <td>If deploy is successful, also unpause the request.</td>
    </tr>
    <tr>
        <td><a href="#SingularityDeploy">SingularityDeploy</a></td>
        <td>required</td>
        <td>The Singularity deploy object</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a></td>
        <td>optional</td>
        <td>User owning this deploy.</td>
    </tr>
</table>



## <a name="SingularityRequest">SingularityRequest</a>

<table border="1">
    <tr>
        <th>type</th>
        <th>required</th>
        <th>description</th>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Boolean&gt;">com.google.common.base.Optional&lt;java.lang.Boolean&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;com.hubspot.singularity.SlavePlacement&gt;">com.google.common.base.Optional&lt;com.hubspot.singularity.SlavePlacement&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Boolean&gt;">com.google.common.base.Optional&lt;java.lang.Boolean&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;">com.google.common.base.Optional&lt;java.util.List&lt;java.lang.String&gt;&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Integer&gt;">com.google.common.base.Optional&lt;java.lang.Integer&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Boolean&gt;">com.google.common.base.Optional&lt;java.lang.Boolean&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Long&gt;">com.google.common.base.Optional&lt;java.lang.Long&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Integer&gt;">com.google.common.base.Optional&lt;java.lang.Integer&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;com.hubspot.singularity.ScheduleType&gt;">com.google.common.base.Optional&lt;com.hubspot.singularity.ScheduleType&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td>string</td>
        <td>optional</td>
        <td>-</td>
    </tr>
</table>



## <a name="SingularityRequestInstances">SingularityRequestInstances</a>

<table border="1">
    <tr>
        <th>type</th>
        <th>required</th>
        <th>description</th>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.Integer&gt;">com.google.common.base.Optional&lt;java.lang.Integer&gt;</a></td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td>string</td>
        <td>optional</td>
        <td>-</td>
    </tr>
</table>



## <a name="SingularityWebhook">SingularityWebhook</a>

<table border="1">
    <tr>
        <th>type</th>
        <th>required</th>
        <th>description</th>
    </tr>
    <tr>
        <td><a href="#WebhookType">WebhookType</a></td>
        <td>optional</td>
        <td>Webhook type (TASK, REQUEST, DEPLOY). Allowable
        values:TASK, REQUEST, DEPLOY</td>
    </tr>
    <tr>
        <td>string</td>
        <td>optional</td>
        <td>URI to POST to.</td>
    </tr>
    <tr>
        <td><a href="#com.google.common.base.Optional&lt;java.lang.String&gt;">com.google.common.base.Optional&lt;java.lang.String&gt;</a></td>
        <td>optional</td>
        <td>User that created webhook.</td>
    </tr>
    <tr>
        <td>long</td>
        <td>optional</td>
        <td>-</td>
    </tr>
    <tr>
        <td>string</td>
        <td>optional</td>
        <td>Unique ID for webhook.</td>
    </tr>
</table>


