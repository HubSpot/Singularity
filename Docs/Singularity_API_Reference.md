# Singularity API

## Requests - /requests

[Requests](objects.md#singularity-request) are the core object in Singularity that represent a request to run some service or scheduled job in Mesos (with tasks).

### Basic Operations

#### Creating or Updating a Request

Validates and creates a new request or update the fields on an existing request. Will return a 400 if the request is invalid. Will unpause the request if there is a paused request with this id. Immediately adds this request to the pending queue to be scheduled.

- Endpoint: **/requests**
- Method: **POST**
- Example: http POST localhost:5060/singularity/v1/requests @request.json
- Response: Singularity Request Parent object

request.json: `{ "id" : "test-singularity-1" }`

#### Posting a Deploy

Validates and creates a new pending deploy object for a Request. Requests may only have a single pending deploy at a time.

- Endpoint: **/requests/request/{requestId}/deploy**
- Method: **POST**
- Example: http POST localhost:5060/singularity/v1/requests/request/test-singularity-1/deploy @deploy.json
- Response: Singularity Request Parent object

deploy.json: `{ "id" : "deploy1", "requestId" : "test-singularity-1", "command" : "sleep 1000" }`

#### Deleting a Request

Deletes the request and schedules its tasks for cleanup (they will be TASK_KILLED)

- Endpoint: **/requests/request/{requestId}**
- Method: **DELETE**
- Query Params: 
  - User: (Optional) The user which requested the delete (this is stored in the Request history for audit purposes)
- Example: http DELETE localhost:5060/singularity/v1/requests/request/test-singularity-1
- Response: Deleted Request object

### Fetching Requests

#### Fetch Single Request

- Endpoint: **/requests/request/{requestId}**
- Method: **GET**
- Example: http GET localhost:5060/singularity/v1/requests/request/test-singularity-1
- Response: Original posted Request object

#### Fetch All Active Requests

- Endpoint: **/requests/active**
- Method: **GET**
- Example: http GET localhost:5060/singularity/v1/requests/active
- Response: List of **all active** Request objects

#### Fetch All Paused Requests

- Endpoint: **/requests/paused**
- Method: **GET**
- Example: http GET localhost:5060/singularity/v1/requests/paused
- Response: List of **all paused** Request objects

#### Fetch All Pending Requests

Pending requests are Requests which gone through a state change that has yet to be acted on by Singularity.

- Endpoint: **/requests/queued/pending**
- Method: **GET**
- Example: http GET localhost:5060/singularity/v1/requests/queued/pending
- Response: List of **all pending** PendingRequestId objects

#### Fetch All Cleaning Requests

Cleaning requests are Requests which are either being deleted or paused.

- Endpoint: **/requests/queued/cleanup**
- Method: **GET**
- Example: http GET localhost:5060/singularity/v1/requests/queued/cleanup
- Response: List of **all cleaning** RequestCleanup objects

### Manipulating Requests

#### Bounce Request

Instruct Singularity to relaunch tasks for all instances of this Request. Old tasks will be killed only after a grace period expires and all new tasks have been alive for that period. Therefore, this Request will temporarily have more than the requested number of instances running.

- Endpoint: **/requests/request/{requestId}/bounce**
- Method: **POST**
- Example: http POST localhost:5060/singularity/v1/requests/request/test-singularity-1/bounce

#### Schedule Request Immediately

Instruct Singularity to immediately run this scheduled task, ignoring its schedule. This new scheduled task will not start running until the current task (if there is one) finishes or fails.

- Endpoint: **/requests/request/{requestId}/run**
- Method: **POST**
- Example: http POST localhost:5060/singularity/v1/requests/request/test-singularity-1/run

#### Pause Request

Pause the Request. Singularity will instruct Mesos to kill all active tasks for this Request and not schedule any new tasks until this request is either unpaused or updated.

- Endpoint: **/requests/request/{requestId}/pause**
- Method: **POST**
- Query Params: 
  - User: (Optional) The user which requested the pause (this is stored in the Request history for audit purposes)
- Example: http POST localhost:5060/singularity/v1/requests/request/test-singularity-1/pause

#### Unpause Request

Unpause the Request, rescheduling tasks for execution.

- Endpoint: **/requests/request/{requestId}/unpause**
- Method: **POST**
- Query Params: 
  - User: (Optional) The user which requested the unpause (this is stored in the Request history for audit purposes)
- Example: http POST localhost:5060/singularity/v1/requests/request/test-singularity-1/unpause

#### Delete Paused Request

Use this endpoint to delete Requests which have been paused.

- Endpoint: **/requests/request/{requestId}/paused**
- Method: **DELETE**
- Query Params: 
  - User: (Optional) The user which deleted the paused Request (this is stored in the Request history for audit purposes)
- Example: http DELETE localhost:5060/singularity/v1/requests/request/test-singularity-1/paused
