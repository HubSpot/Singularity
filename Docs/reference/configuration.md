# Singularity Configuration #

Singularity (Service) is configured by DropWizard via a YAML file referenced on the command line. Top-level configuration elements reside at the root of the configuration file alongside [DropWizard configuration](https://dropwizard.github.io/dropwizard/manual/configuration.html). 

- [Root Configuration](#root-configuration)
  - [Common Configuration](#common-configuration)
    - [General](#general)
    - [Healthchecks and New Task Checks](#healthchecks-and-new-task-checks)
    - [Limits](#limits)
    - [Cooldown](#cooldown)
    - [Load Balancer API](#load-balancer-api)
    - [User Interface](#user-interface)
  - [Internal Scheduler Configuration](#internal-scheduler-configuration)
    - [Pollers](#pollers)
    - [Mesos](#mesos)
    - [Thread Pools](#thread-pools)
    - [Operational](#operational)
- [Mesos Configuration](#mesos-configuration)
 - [Framework](#framework)
 - [Resource Limits](#resource-limits)
 - [Racks](#racks)
 - [Slaves](#slaves)

## Root Configuration ##

### Common Configuration ###

These are settings that are more likely to be altered.

#### General ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| allowRequestsWithoutOwners | true | If false, submitting a request without at least one owner will return a 400 | boolean |
| commonHostnameSuffixToOmit | null | If specified, will remove this hostname suffix from all taskIds | string |
| defaultSlavePlacement | GREEDY | The slavePlacement strategy when not specified in a request. GREEDY uses whatever slaves are available, SEPARATE_BY_DEPLOY (same as SEPARATE) ensures no 2 instances / tasks of the same request and deploy id are ever placed on the same slave, SEPARATE_BY_REQUEST ensures no two tasks belonging to the same request (regardless if deploy id) are placed on the same host, and OPTIMISTIC attempts to spread out tasks but may schedule some on the same slave | enum / string [GREEDY, OPTIMISTIC, SEPARATE (deprecated), SEPARATE_BY_DEPLOY, SEPARATE_BY_REQUEST]
| defaultValueForKillTasksOfPausedRequests | true | When a task is paused, the API allows for the tasks of that request to optionally not be killed. If that parameter is not set in the pause request, this value is used | boolean |
| deltaAfterWhichTasksAreLateMillis | 30000 (30 seconds) | The amount of time after a task's schedule time that Singularity will classify it (in state API and dashboard) as a late task | long | 
| deployHealthyBySeconds | 120 | Default amount of time to allow pending deploys to run for before transitioning them into active deploys. If more than this time passes before a deploy can be considered healthy (all of its tasks either make it to TASK_RUNNING or pass healthchecks), then the deploy will be rejected | long |
| killNonLongRunningTasksInCleanupAfterSeconds | 86400 (1 day) | Kills scheduled and one-off tasks after this amount of time if they have been scheduled for cleaning (a new deploy succeeds, the underlying slave is decomissioned) | long | 
| hostname | null | Hostname of this Singularity instance | string |

#### Healthchecks and New Task Checks ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| considerTaskHealthyAfterRunningForSeconds | 5 | Tasks which make it to TASK_RUNNING and run for at least this long (that are not health-checked) are considered healthy | long | 
| healthcheckIntervalSeconds | 5 | Default amount of time to wait in between attempting task healthchecks | long |
| healthcheckTimeoutSeconds | 5 | Default amount of time to wait for healthchecks to return before considering them failed | long | 
| killAfterTasksDoNotRunDefaultSeconds | 600 (10 minutes) | Amount of time after which new tasks (that are not part of a deploy) will be killed if they do not enter TASK_RUNNING | long | 

#### Limits ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| maxDeployIdSize | 50 | Deploy ids over this size will cause deploy requests to fail with 400 | int | 
| maxRequestIdSize | 100 | Request ids over this size will cause new requests to fail with 400 | int | 

#### Cooldown ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| cooldownAfterFailures | 3 | The number of sequential failures after which a request is placed into system cooldown (a delay is added to newly scheduled tasks) - set to 0 to disable cooldown | int |
| cooldownAfterPctOfInstancesFail | 1.0 | The percentage of instances which must fail at least cooldownAfterFailures times to cause the request to enter system cooldown | double |
| cooldownExpiresAfterMinutes | 15 | The window used to evaluate task failures. Tasks must fail at least cooldownAfterFailures times during this amount of time to enter system cooldown - set to 0 to disable cooldown | long | 
| cooldownMinScheduleSeconds | 120 | When a request enters cooldown, new tasks are delayed by at least this long | long | 

#### Load Balancer API ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| loadBalancerQueryParams | null | Additional query parameters to pass to the Load Balancer API | Map<String, String> | 
| loadBalancerRequestTimeoutMillis | 2000 | The timeout for making API calls to the Load Balancer API (these will be retried) | long |
| loadBalancerUri | null | The URI of the Load Balancer API (Baragon) | string |

#### User Interface ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| sandboxDefaultsToTaskId | false | If true, the Singularity API will return the sandbox view of root/taskId when queried without a path (Useful when using SingularityExecutor) | boolean |
| enableCorsFilter | false | If true, provides a Bundle which will enable CORS | boolean | 

### Internal Scheduler Configuration ###

These settings are less likely to be changed, but were included in the configuration instead of hardcoding values. 

#### Pollers ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| checkDeploysEverySeconds | 5 | Check the status (health) of pending deploys, promoting them to active or removing them on this interval | long |
| checkNewTasksEverySeconds | 5 | Check the health of new (non-deployed, non-healthchecked) tasks to make sure they eventually get to running on this interval | long | 
| checkSchedulerEverySeconds | 5 | Runs scheduler checks (processes decommissions and pending queue) on this interval (these tasks also run when an offer is received) | long | 
| checkWebhooksEveryMillis | 10000 (10 seconds) | Will check for and send new queued webhooks on this interval | long | 
| cleanupEverySeconds | 5 | Will cleanup request, task, and other queues on this interval | long | 
| persistHistoryEverySeconds | 3600 (1 hour) | Moves stale historical task data from ZooKeeper into MySQL, setting to 0 will disable history persistence | long |
| saveStateEverySeconds | 60 | State about this Singularity instance is saved (available over API) on this interval | long |

#### Mesos ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| checkReconcileWhenRunningEveryMillis | 30000 (30 seconds) | When reconciling tasks, will re-request task updates on this interval until reconciliation finishes | long |
| startNewReconcileEverySeconds | 600 (10 minutes) | Starts a new reconciliation cycle (if one is not currently running) on this interval (A relatively costly operation that detects updates Mesos failed to deliver) | long | 
| askDriverToKillTasksAgainAfterMillis | 300000 (5 minutes) | Amount of time to wait before instruction mesos to kill a task which has been killed by Singularity but is still running | long |

#### Thread Pools ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| checkNewTasksScheduledThreads | 3 | Max number of threads to use to check new tasks | int |
| healthcheckStartThreads | 3 | Max number of threads to use to start healthchecks | int |
| logFetchMaxThreads | 15 | Max number of threads to use to fetch log directories from Mesos REST API | int | 

#### Operational ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| closeWaitSeconds | 5 | Will wait at least this many seconds when shutting down thread pools | long | 
| compressLargeDataObjects | true | Will compress larger objects inside of ZooKeeper and MySQL | boolean |
| maxHealthcheckResponseBodyBytes | 8192 | Number of bytes to save from healthcheck responses (displayed in UI) | int | 
| maxQueuedUpdatesPerWebhook | 50 | Max number of updates to queue for a given webhook url, after which some webhooks will not be delivered | int | 
| zookeeperAsyncTimeout | 5000 | Milliseconds for ZooKeeper timeout. Calls to ZooKeeper which take over this timeout will cause the operations to fail and Singularity to abort | long | 
| cacheStateForMillis | 30000 (30 seconds) | Amount of time to cache internal state for when requested over API | long |
| sandboxHttpTimeoutMillis | 5000 (5 seconds) | Sandbox HTTP calls will timeout after this amount of time (fetching logs for emails / UI)
| newTaskCheckerBaseDelaySeconds | 1 | Added to the the amount of deploy to wait before checking a new task | long | 
| allowTestResourceCalls | false | If true, allows calls to be made to the test resource, which can test internal methods | boolean |

## Mesos Configuration ##

These settings should live under the "mesos" field inside the root configuration.

#### Framework ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| master | null | | string |
| frameworkName | null | | string |
| frameworkId | null | | string |
| frameworkFailoverTimeout | 0.0 | | double |
| checkpoint | false | | boolean |


#### Resource Limits ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| defaultCpus | 1 | Number of CPUs to request for a task if none are specified | int | 
| defaultMemory | 64 | MB of memory to request for a task if none is specified | int |
| maxNumInstancesPerRequest | 25 | Max instances (tasks) to allow for a request (requests using over this will return a 400) | int |
| maxNumCpusPerInstance | 50 | Max number of CPUs allowed on a given task | int | 
| maxNumCpusPerRequest | 900 | Max number of CPUs allowed for a given request (cpus per task * task instance) | int | 
| maxMemoryMbPerInstance | 24000 | Max MB of memory allowed on a given task | int | 
| maxMemoryMbPerRequest | 450000 | Max MB of memory allowed for a given request (memoryMb per task * task instances) | int | 

#### Racks ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| rackIdAttributeKey | rackid | The Mesos slave attribute to denote a rack | string |
| defaultRackId | DEFAULT | The rackId to assign to a slave if no rackId attribute value is present | string | 

#### Slaves ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| slaveHttpPort | 5051 | The port to talk to slaves on | int |
| slaveHttpsPort | absent | The HTTPS port to talk to slaves on | Integer (Optional) | 

