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
| excludeMetadata | false | Exclude custom object metadata | boolean |
| list | false | Do not generate download/get urls, only list the files and metadata | boolean |

###### Response
[List[SingularityS3LogMetadata]](models.md#model-SingularityS3LogMetadata)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/logs/search`

Retrieve a paginated list of logs stored in S3


###### Parameters
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | true |  | [SingularityS3SearchRequest](models.md#model-linkType)</a> |

###### Response
[SingularityS3SearchResult](models.md#model-SingularityS3SearchResult)


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
| excludeMetadata | false | Exclude custom object metadata | boolean |
| list | false | Do not generate download/get urls, only list the files and metadata | boolean |
| maxPerPage | false | Max number of results to return per bucket searched | int |

###### Response
[List[SingularityS3LogMetadata]](models.md#model-SingularityS3LogMetadata)


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
| excludeMetadata | false | Exclude custom object metadata | boolean |
| list | false | Do not generate download/get urls, only list the files and metadata | boolean |
| maxPerPage | false | Max number of results to return per bucket searched | int |

###### Response
[List[SingularityS3LogMetadata]](models.md#model-SingularityS3LogMetadata)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -