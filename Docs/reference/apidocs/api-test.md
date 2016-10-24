#### Overview
Misc testing endpoints.

#### **POST** `/api/test/stop`

Stop the Mesos scheduler driver.


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/start`

Start the Mesos scheduler driver.


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/scheduler/statusUpdate/{taskId}/{taskState}`

Force an update for a specific task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true |  | string |
| taskState | true |  | string |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/reconcile`

Start task reconciliation


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/notleader`

Make this instanceo of Singularity believe it&#39;s lost leadership.


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/leader`

Make this instance of Singularity believe it&#39;s elected leader.


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/exception`

Trigger an exception.


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| message | false |  | string |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/test/abort`

Abort the Mesos scheduler driver.


###### Parameters
- No parameters

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -