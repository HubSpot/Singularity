### Expiring Actions

Released in `0.4.9`

### Action expiration + additional action metadata
Some actions in Singularity now have the concept of expiration (as in, giving up after a certain period of time). Corresponding endpoints have been updated to accept more information about action expiration and action metadata.

#### Rack and slave operations
- `/racks/rack/{rackId}/decommission`
- `/racks/rack/{rackId}/freeze`
- `/racks/rack/{rackId}/activate`
- `/slaves/slave/{slaveId}/decommission`
- `/slaves/slave/{slaveId}/freeze`
- `/slaves/slave/{slaveId}/activate`

These URLs accept a JSON object with this format:

| name | type | required | description |
|------|------|----------|-------------|
| message | string | optional | A message to show to users about why this action was taken |

#### Request bounce
- `/requests/request/{requestId}/bounce`

This URL accepts a JSON object with this format:

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | Instruct replacement tasks for this bounce only to skip healthchecks |
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |
| incremental | boolean | optional | If present and set to true, old tasks will be killed as soon as replacement tasks are available, instead of waiting for all replacement tasks to be healthy |

#### Scheduling a request to run immediately
- `/requests/request/{requestId}/run`

This URL accepts a JSON object with this format:

| name | type | required | description |
|------|------|----------|-------------|
| runId | string | optional | An id to associate with this request which will be associated with the corresponding launched tasks |
| skipHealthchecks | boolean | optional | If set to true, healthchecks will be skipped for this task run |
| commandLineArgs | Array[string] | optional | Command line arguments to be passed to the task |
| message | string | optional | A message to show to users about why this action was taken |

#### Unpausing a request
- `/requests/request/{requestId}/unpause`

This URL accepts a JSON object with this format:

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | If set to true, instructs new tasks that are scheduled immediately while unpausing to skip healthchecks |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |

#### Exit request cooldown
- `/requests/request/{requestId}/exit-cooldown`

This URL accepts a JSON object with this format:

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | Instruct new tasks that are scheduled immediately while executing cooldown to skip healthchecks |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |

#### Deleting a request
- `/requests/request/{requestId}`

This URL accepts a JSON object with this format:

| name | type | required | description |
|------|------|----------|-------------|
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |

#### Killing a task
- `/tasks/task/{taskId}`

This URL accepts a JSON object with this format:

| name | type | required | description |
|------|------|----------|-------------|
| waitForReplacementTask | boolean | optional | If set to true, treats this task kill as a bounce - launching another task and waiting for it to become healthy |
| override | boolean | optional | If set to true, instructs the executor to attempt to immediately kill the task, rather than waiting gracefully |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |

#### Scaling requests
- `/requests/request/{requestId}/scale` (previously `/requests/request/{requestId}/instances`)

This URL accepts a JSON object with this format:

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | If set to true, healthchecks will be skipped while scaling this request (only) |
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |
| instances | int | optional | The number of instances to scale to |

#### Pausing a request
- `/requests/request/{requestId}/pause`

This URL accepts a JSON object with this format:

| name | type | required | description |
|------|------|----------|-------------|
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| killTasks | boolean | optional | If set to false, tasks will be allowed to finish instead of killed immediately |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |

**NOTE:** The `user` field has been removed from this object.

#### Disabling request healthchecks
- `/requests/request/{requestId}/skip-healthchecks`

This URL accepts a JSON object with this format:

| name | type | required | description |
|------|------|----------|-------------|
| skipHealthchecks | boolean | optional | If set to true, healthchecks will be skipped for all tasks for this request until reversed |
| durationMillis | long | optional | The number of milliseconds to wait before reversing the effects of this action (letting it expire) |
| message | string | optional | A message to show to users about why this action was taken |
| actionId | string | optional | An id to associate with this action for metadata purposes |

### New endpoints for cancelling actions
These endpoints were added in order to support cancelling certain actions:
- `DELETE /requests/request/{requestId}/scale` -- Cancel an expiring scale
- `DELETE /requests/request/{requestId}/skip-healthchecks` -- Cancel an expiring skip healthchecks override
- `DELETE /request/{requestId}/pause` -- Cancel (unpause) an expiring pause
- `DELETE /request/{requestId}/bounce` -- Cancel a bounce