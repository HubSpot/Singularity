# Load Balancers

Singularity supports integration with a Load Balancer API (LB API) like [Baragon](https://github.com/HubSpot/Baragon) for the purpose of coordinating deploys and normal task operations.

## Requirements

- Provide a loadBalancerUri in the configuration yaml
- On request creation, set loadBalanced to true

## How it works

### Request

Singularity POSTs a LoadBalancerRequest (LBR) with an id (LBR ID) and tasks to add and/or remove from load balancers. Singularity expects the LB API to be asynchronous and store state about operations using the provided LBR ID, which the LB API relies on Singularity to supply and should not infer context from. Singularity expects the LB API to respond to all LBR (POST, GET, or DELETE) with a LoadBalancerResponse JSON object which has the following fields:

- LoadBalancerState (LBS) (one of FAILED, WAITING, SUCCESS, CANCELING, CANCELED, INVALID_REQUEST_NOOP)
- LoadBalancerRequestId (echos back the LBR ID)

Singularity makes a POST request to start a change of state (add or remove tasks from load balancers), but can handle any LBS response from any request. 
Singularity makes a DELETE request to request a cancel of a previously requested POST.

### Edge cases

- Singularity may make multiple POST or DELETE requests to the same LBR ID, especially if the LB API responds with a failure status code or does not respond quickly enough (configurable in Singularity.)
- Singularity may make a DELETE request to an LBR ID which has already succeeded, in which case the LB API should return SUCCESS, which is the state of the LBR, not the response to the DELETE request.
- Singularity may make a POST request to add a task on a different LBR ID then the LBR ID it uses to remove that task. 
- Singularity assumes that CANCELED = FAILED in terms of the final result of the LBR.
- Singularity may request removal of a task that is not actually load balanced or has already been removed from load balancers. In this case, the LB API should return a status code of SUCCESS to indicate that all is well.

## When it works

### Deploys

Once all of the tasks in a new deploy are healthy (have entered TASK_RUNNING and have passed healthchecks, IF present), Singularity posts a LBR to add the new deploy tasks and to remove all other tasks associated with this request. The LBR ID is requestId-deployId. 

Singularity will poll (a GET on the LBR) until it receives either a SUCCESS, FAILED, or CANCELED state. While Singularity is polling, it may exceed the deadline allowed for the deploy, in which case it will make a DELETE request to the LBR, which signifies its desire to cancel the LB update. Regardless, Singularity will continue to poll until it receives one of the 3 final states and will update the deploy accordingly.

### New Tasks

When a new task starts for reasons other than a new deploy, such as to replace a failed task or due to scaling, Singularity will attempt to add that task to the LB API as part of its NewTaskChecker process. The NewTaskChecker ensures that new tasks eventually make it into TASK_RUNNING and are added to the LB API (if loadBalanced is true for that Request.) Once a new task has passed healthchecks (if necessary), Singularity will make a POST request to the LBR with the LBR ID taskId-ADD. 

Singularity will poll the GET request for this LBR until it receives a final state. If it gets a CANCELED or FAILED it will kill the new task (the scheduler should request a new one to take its place.)

### Task failures

When a task fails, is lost, or killed, Singularity will add it to a queue to ensure that it is removed from the LB API if it was ever added in the first place. 

Singularity will make a POST request using the LBR ID taskId-REMOVE. It will continue to make GET and POST requests to this LBR ID until it is successfully removed (SUCCESS.)

### Graceful task cleanup (decommissions, bounces)

Singularity will attempt to gracefully decommission tasks from the LB API when those tasks are being killed from within Singularity.

Singularity will make a POST request using the LBR ID taskId-REMOVE. It will continue to make GET and POST requests to this LBR ID until it is successfully removed (SUCCESS.)

Singularity will not kill the task until it is removed from the LB API.
