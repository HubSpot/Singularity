#### Overview
Manages Singularity task logs stored in S3.

#### **GET** `/api/logs/task/{taskId}`

Retrieve the list of logs stored in S3 for a specific task.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| taskId | true | The task ID to search for | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| start | false | Start timestamp (millis, 13 digit) | long |
| end | false | End timestamp (mills, 13 digit) | long |

###### Response
[List[SingularityS3Log]](#model-SingularityS3Log)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/logs/request/{requestId}/deploy/{deployId}`

Retrieve the list of logs stored in S3 for a specific deploy.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to search for | string |
| deployId | true | The deploy ID to search for | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| start | false | Start timestamp (millis, 13 digit) | long |
| end | false | End timestamp (mills, 13 digit) | long |

###### Response
[List[SingularityS3Log]](#model-SingularityS3Log)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/logs/request/{requestId}`

Retrieve the list of logs stored in S3 for a specific request.


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| requestId | true | The request ID to search for | string |
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| start | false | Start timestamp (millis, 13 digit) | long |
| end | false | End timestamp (mills, 13 digit) | long |

###### Response
[List[SingularityS3Log]](#model-SingularityS3Log)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -