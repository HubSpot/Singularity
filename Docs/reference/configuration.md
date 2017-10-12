# Singularity Configuration #

Singularity (Service) is configured by DropWizard via a YAML file referenced on the command line. Top-level configuration elements reside at the root of the configuration file alongside [DropWizard configuration](https://dropwizard.github.io/dropwizard/manual/configuration.html). 

- [SingularityService Configuration](#root-configuration)
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
  - [Network Configuration](#network-configuration)
  - [Database](#database)
    - [History Purging](#history-purging)
  - [S3](#s3)
  - [Sentry](#sentry)
  - [Email/SMTP](#smtp)
  - [UI Configuration](#ui-configuration)

## Root Configuration ##

### Common Configuration ###

These are settings that are more likely to be altered.

#### General ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| allowRequestsWithoutOwners | true | If false, submitting a request without at least one owner will return a 400 | boolean |
| commonHostnameSuffixToOmit | null | If specified, will remove this hostname suffix from all taskIds | string |
| defaultSlavePlacement | GREEDY | See [Slave Placement](../about/how-it-works.md#user-content-placement) | enum / string [GREEDY, OPTIMISTIC, SEPARATE (deprecated), SEPARATE_BY_DEPLOY, SEPARATE_BY_REQUEST, SPREAD_ALL_SLAVES]
| defaultValueForKillTasksOfPausedRequests | true | When a task is paused, the API allows for the tasks of that request to optionally not be killed. If that parameter is not set in the pause request, this value is used | boolean |
| deltaAfterWhichTasksAreLateMillis | 30000 (30 seconds) | The amount of time after a task's schedule time that Singularity will classify it (in state API and dashboard) as a late task | long | 
| deployHealthyBySeconds | 120 | Default amount of time to allow pending deploys to run for before transitioning them into active deploys. If more than this time passes before a deploy can be considered healthy (all of its tasks either make it to TASK_RUNNING or pass healthchecks), then the deploy will be rejected | long |
| killNonLongRunningTasksInCleanupAfterSeconds | 86400 (1 day) | Kills scheduled and one-off tasks after this amount of time if they have been scheduled for cleaning (a new deploy succeeds, the underlying slave is decomissioned) | long | 
| hostname | null | Hostname of this Singularity instance | string |

#### Healthchecks and New Task Checks ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| considerTaskHealthyAfterRunningForSeconds | 5 | Tasks which make it to TASK_RUNNING and run for at least this long (that are not health-checked) are considered healthy | long | 
| healthcheckIntervalSeconds | 5 | Default amount of time to wait in between attempting task healthchecks | int |
| healthcheckTimeoutSeconds | 5 | Default amount of time to wait for healthchecks to return before considering them failed | int | 
| killAfterTasksDoNotRunDefaultSeconds | 600 (10 minutes) | Amount of time after which new tasks (that are not part of a deploy) will be killed if they do not enter TASK_RUNNING | long |
| healthcheckMaxRetries | | Default max number of time to retry a failed healthcheck for a task before considering the task to be unhealthy | int |
| startupDelaySeconds | | By default, wait this long before starting any healthchecks on a task | int |
| startupTimeoutSeconds | 45 | If a healthchecked task has not responded with a valid http response in `startupTimeoutSeconds` consider it unhealthy | int |
| startupIntervalSeconds | 2 | In the startup period (before a valid http response has been received) wait this long between healthcheck attempts | int |
| healthcheckFailureStatusCodes | [] | If any of these status codes is received during a healthcheck, immediately consider the task unhealthy, do not retry the check | List<Integer> |

#### Deploys ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| defaultDeployStepWaitTimeMs | 0 | If using an incremental deploy, wait this long between deploy steps if not specified in the deploy | int |
| defaultDeployMaxTaskRetries | 0 | Allow this many tasks to fail and be retried before failing a new deploy | int |

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
| deleteRemovedRequestsFromLoadBalancer | false | If a request is removed from Singularity, issue a `DELETE` to the load balancer for that service | boolean |

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
| checkJobsEveryMillis | 600000 (10 mins) | Check for jobs running longer than the expected time on this interval | long |
| checkExpiringUserActionEveryMillis | 45000 | Check for expiring actions that should be expired on this interval | long |

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
| deleteDeploysFromZkWhenNoDatabaseAfterHours | 336 (14 days) | Delete deploys from zk when they are older than this if we are not using a database | long |
| maxStaleDeploysPerRequestInZkWhenNoDatabase | infinite (disabled) | Delete oldest deploys from zk when there are more than this number for a given request, if we're not already persisting them to a database | int |
| deleteStaleRequestsFromZkWhenNoDatabaseAfterHours | 336 (14 days) | Delete stale requests after this amount of time if we are not using a database | long |
| maxRequestsWithHistoryInZkWhenNoDatabase | infinite (disabled) | Delete history of oldest requests from zk when there are more than this number of requests, if we're not already persisting them to a database | int |
| deleteTasksFromZkWhenNoDatabaseAfterHours | 168 (7 days) | Delete old tasks from zk after this amount of time if we are not using a database | long |
| maxStaleTasksPerRequestInZkWhenNoDatabase | infinite (disabled) | Delete oldest tasks from zk when there are more than this number for a given request, if we're not already persisting them to a database | int |
| taskPersistAfterStartupBufferMillis | 60000ms (1 min) | Wait this long after a task starts before persisting it in history | long |
| deleteDeadSlavesAfterHours | 168 (7 days) | Remove dead slaves from the list after this amount of time | long |
| deleteUndeliverableWebhooksAfterHours | 168 (7 days) | Delete (and stop retrying) failed webhooks after this amount of time | long |
| waitForListeners | true | If true, the event system waits for all listeners having processed an event. | boolean |
| warnIfScheduledJobIsRunningForAtLeastMillis | 86400000 (1 day) | Warn if a scheduled job has been running for this long | long |
| warnIfScheduledJobIsRunningPastNextRunPct | 200 | Warn if a scheduled job has run this much past its next scheduled run time (e.g. 200 => ran through next two run times) | int |
| pendingDeployHoldTaskDuringDecommissionMillis | 600000ms (10 minutes) | Don't kill tasks on a decommissioning slave that are part of a pending deploy for this amount of time to allow the deploy to complete | long |
| defaultBounceExpirationMinutes | 60 | Expire a bounce after this many minutes if an expiration is not provided in the request to bounce | int |
| cacheOffers | false | Hold on to unused offers for up to `cacheOffersForMillis` | boolean |
| cacheOffersForMillis | If `cacheOffers` is true, decline offers after this amount of time if they ahve not been used | long |
| offerCacheSize | The maximum number of offers to cache at once | int |

## Mesos Configuration ##

These settings should live under the "mesos" field inside the root configuration.

#### Framework ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| master | null | A comma separated list of mesos master `host:port` | string |
| frameworkName | null | | string |
| frameworkId | null | | string |
| frameworkFailoverTimeout | 0.0 | | double |
| frameworkRole | null | Specify framework's desired role when Singularity registers with the master | String |
| checkpoint | true | | boolean |
| credentialPrincipal | | Enable framework auth by setting both this and credentialSecret | String |
| credentialSecret | | Enable framework auth by setting both this and credentialPrincipal | String |

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

#### Offers ####
| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| longRunningUsedCpuWeightForOffer | 0.30 | The weight long running tasks' cpu utilization carries when scoring an offer (should add up to 1 with longRunningUsedMemWeightForOffer) | double |
| longRunningUsedMemWeightForOffer | 0.70 | The weight long running tasks' memory utilization carries when scoring an offer (should add up to 1 with longRunningUsedCpuWeightForOffer) | double |
| freeCpuWeightForOffer | 0.30 | The weight the slave's free cpu carries when scoring an offer (should add up to 1 with freeMemWeightForOffer) | double |
| freeMemWeightForOffer | 0.70 | The weight the slave's free memory carries when scoring an offer (should add up to 1 with freeCpuWeightForOffer) | double |
| defaultOfferScoreForMissingUsage | 0.30 | The default offer score used for offers without utilization metrics | double |
| considerNonLongRunningTaskLongRunningAfterRunningForSeconds | 21600 (6 hours) | If a non long running task runs, on average, this long or more, it's considered a long running task | long |
| maxNonLongRunningUsedResourceWeight | 0.50 | The max weight long running tasks' utilization can carry when scoring a non long running task for an offer | double

## Database ##

| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| database | | The database connection for SingularityService follows the [dropwizard DataSourceFactory format](http://www.dropwizard.io/0.7.0/dropwizard-db/apidocs/io/dropwizard/db/DataSourceFactory.html) | [DataSourceFactory](http://www.dropwizard.io/0.7.0/dropwizard-db/apidocs/io/dropwizard/db/DataSourceFactory.html) |

## Network Configuration

These settings should live under the "network" field of the root configuration.

| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| defaultPortMapping | false | If no port mapping is provided, map all Mesos-provided ports to the host | boolean |

#### History Purging ####

These settings live under the "historyPuring" field in the root configuration

| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| deleteTaskHistoryAfterDays | 365 | Purge tasks older than this many days | int |
| deleteTaskHistoryAfterTasksPerRequest | 10000 | Purge oldest tasks when there are more than this many associated with a single request | int |
| deleteTaskHistoryBytesInsteadOfEntireRow | true | Only delete the taskHistoryBytes instead of the entire record of the task (e.g. to save space)| boolean |
| checkTaskHistoryEveryHours | 24 | Run the purge every x hours | int |
| enabled | false | Should we run the database purge | boolean |

## S3 ##

These settings live under the "s3" field in the root configuration. If using the SingularityS3Uploader, this section will need to be provided in order to view lists of and download s3 logs from the SingularityUI.

| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| maxS3Thread | 3 | Max threads to run for fetching logs from s3 | int |
| waitForS3ListSeconds | 5 | Timeout in seconds for fetching list of s3 logs | int |
| waitForS3LinksSeconds | 1 | Timeout in seconds for creating new s3 links | int |
| expireS3LinksAfterMillis | 86400000 (1 day) | Expire generated s3 log links after this amount of time | long |
| s3Bucket | | S3 bucket to search for logs | String |
| groupOverrides | | Extra s3 configurations provided such that individual requests may use separate s3 buckets. Each S3GroupOverrideConfiguration has a name specified by the Map key and consists of an s3Bueckt, s3AccessKey, and s3SecretKey |Map<String, S3GroupOverrideConfiguration> |
| s3KeyFormat | | Search for logs with keys in this format, should be the same as the key format set in the SingularityS3Uploader | String |
| s3AccessKey | | aws access key for the specified s3 bucket | String |
| s3SecretKey | | aws secret key for the specified s3 bucket | String |
| missingTaskDefaultS3SearchPeriodMillis | 259200000ms (3 days) | Search over this many days for s3 logs when no task data is found | long |

## Sentry ##

These settings live under the "sentry" field in the root config and enable Singularity error reporting to [sentry](https://getsentry.com/welcome/).

| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| dsn | | Sentry DSN (Data Source Name) | String |
| prefix| "" | Prefix string for event culprit naming and messages | String |

## SMTP ##

These settings live under the "smtp" field in the root config.

| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| username | | smtp username | String |
| password | | smtp password | String |
| taskLogLength | 512 | Send this many lines of a tasks log in emails | int |
| host | localhost | Host for smtp session | String |
| port | 25 | Port for smtp session | int |
| from | "singularity-no-reply@example.com" | Send emails form this address | String |
| mailMaxThreads | 3 | max threads for email sending process | int |
| admins | [] | List of admin user emails | List\<String\> |
| rateLimitAfterNotifications | 5 | Rate limit email sending after this many notifications have been sent in `rateLimitPeriodMillis` | int |
| rateLimitPeriodMillis | 60000 (10 mins) | time period for `rateLimitAfterNotifications` | long |
| rateLimitCooldownMillis | 3600000 (1 hour) | Cooldown time before rate limiting is removed | long |
| taskEmailTailFiles | [stdout, stderr] | Send the tail of these files in messages about tasks | List\<String\> |
| emails | See below | See below | Map\<EmailType, List\<EmailDestination\>\> |
| subjectPrefix | unset | String prepended to the email subject line | String |
| ssl | false | Connect to SMTP host over ssl | boolean |

You may need `libmail-java` installed on your Singularity master host in order to connect to your smtp server.

#### Emails List ####

The emails list determines what emails to send notifications to and for what events. You can specify a map of [`EmailType`](https://github.com/HubSpot/Singularity/blob/master/SingularityBase/src/main/java/com/hubspot/singularity/SingularityEmailType.java)
 to a list of [`EmailDestination`s](https://github.com/HubSpot/Singularity/blob/master/SingularityBase/src/main/java/com/hubspot/singularity/SingularityEmailDestination.java)

`EmailType` corresponds to different events that could trigger emails such as `TASK_LOST` or `TASK_FAILED`

`EmailDestination` corresponds to one of `OWNERS` (as listed on the Singularity Request), `ACTION_TAKER` (user who triggered the action causing the email update), or `ADMINS` (specified in config as seen above)

An email list might look something like
```yaml
smtp:
  emails:
    TASK_LOST:
      - OWNERS
    TASK_FAILED:
      - OWNERS
    TASK_FAILED_DECOMISSIONED:
      - OWNERS
    TASK_KILLED:
      - OWNERS
    TASK_KILLED_DECOMISSIONED:
      - OWNERS
    TASK_KILLED_UNHEALTHY:
      - OWNERS
    TASK_SCHEDULED_OVERDUE_TO_FINISH:
      - OWNERS
    TASK_FINISHED_ON_DEMAND:
      - OWNERS
    TASK_FINISHED_RUN_ONCE:
      - OWNERS
    TASK_FINISHED_SCHEDULED:
      - OWNERS
    TASK_FINISHED_LONG_RUNNING:
      - OWNERS
```

## UI Configuration ##

These settings live under the "ui" field in the root config.

| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| title | "Singularity" | Title shown in the left of the menu bar in ui | String |
| navColor | "" | Color for nav bar | String |
| baseUrl | | Base url where the ui will be hosted (e.g. http://localhost:7099/singularity) | String |
| runningTaskLogPath | stdout | Generate link to this log for running tasks on the request page | String |
| finishedTaskLogPath | stdout | Generate link to this log for finished tasks on the request page | String |
| hideNewDeployButton | false | Don't show the 'New Deploy' button | boolean |
| hideNewRequestButton | false | Don't show the 'New Request' button | boolean |
| rootUrlMode | INDEX_CATCHALL | `INDEX_CATCHALL`: UI is served off of / using a catchall resource. `UI_REDIRECT`: UI is served off of /ui, path and index redirects there. `DISABLED`: UI is served off of /ui and the root resource is not served at all | enum / String `INDEX_CATCHALL`, `UI_REDIRECT`, `DISABLED` |

## Zookeeper ##

These settings live under the "zookeeper" field in the root config.

| Parameter | Default | Description | Type |
|-----------|---------|-------------|------|
| quorum | | Comma separated host:port list of zk hosts | String |
| sessionTimeoutMillis | 600_000 | zookeeper session timeout | int |
| connectTimeoutMillis | 60_000 | Connect to zookeeper timeout | int |
| retryBaseSleepTimeMilliseconds | 1_000 | Wait time between zookeeper connection retries | int |
| retryMaxTries | 3 | Max retries to obtain a zookeeper connection before aborting | int |
| zkNamespace | | Path under which to store Singularity data in zk (e.g. /singularity) | String |
