# Singularity Objects

## Request objects

### Singularity Request

The request object is the basic object in Singularity. In order to run something in Singularity, a request must be posted to the API. A valid request contains just an id. Singularity does not change the Request object (with the exception of a minor schedule modification to be compatible with quartz) and will return back the original or updated Request object on many API calls. It is possible to update a Request by posting a new Request object with an existing id.

#### Basic Fields

- id: (String) (Required) The unique identifier of this request. If this is the same as an existing request it will update that request.
- owners: (String List) A list of email addresses of admins or owners for this request. They will be emailed if requests fail and in other important situations.

#### Long Running Task Fields

Only for use when running services, long running tasks, etc.

- instances: (Number) The number of concurrent instances of this request to run at all times. If this number is changed or if a task fails or finishes, Singularity will attempt to scale up or scale down to the required number of instances.  
- rackSensitive: (Boolean) If true, will only launch tasks evenly across racks. If there are tasks leftover, and not enough racks, for example, 3 racks and 7 instances, one rack will be able to run 3 instances.

#### Scheduled Fields

For cron and scheduled tasks.

- schedule: (String) A cron schedule (which allows seconds, but does not require them) for how often to run this request.
- numRetriesOnFailure: (Number) The number of times to immediately retry this request when it does not complete successfully. If NULL or set to 0, when a scheduled task fails, it will calculate its next run based on the schedule and the current time. When the number of retries is exhausted, it will also calculate the next run based on its schedule and the current time.

### Singularity Deploy

The deploy object belongs to a Request and has information about how to actually run the task. It specifies information used by the Mesos executor.

- id: (String) (required) the id of the deploy. Used in taskIds. Must be unique for the request.
- requestId: (String) (required) the id of the parent request
- resources: ([Resources Object](objects.md#resources)) The number of resources to request for each task or instance this request launches.

#### Default Executor Fields

When using the default Mesos executor.

- command: (String) The command to pass to the default mesos executor, eg. sleep 1000.
- env: (String Object/Map) An object with pairs which will be passed into ENV. Ports will be added to this is requested.
- uris: (String List) A list of objects to download for the default executor.

#### Custom Executor Fields

When using any executor other than the default Mesos executor.

- executor: (String) This the executor command sent to the custom executor. Generally it is the name of the custom executor.
- executorData: (Object) Can be a map or string.  This is what is passed to the executor instead of the command. If it is a map and the request requires ports, the ports will be passed into the map as a number array.

## Misc Objects

### Resources

- cpus (number) - cpus per instance for this request. If this can not be satisfied, it will not run.
- memoryMb (number) - memory in MB per instance for this request. If this can not be satisfied, it will not run.
- numPorts (number) - the number of ports to reserve per instance for this request. If this can not be satisfied, it will not run. Ports are passed into the executor in the environment if using the default mesos executor and in executorData if it is a map and using a custom executor.

## Response objects
