### Disaster Detection

Singularity can be configured to automatically detect 'disaster' scenarios based on a number of indicators and can react in such a way as too limit further stress on the cluster while it is recovering.

Disaster detection can be enabled by adding `enabled: true` to the `disasterDetection` portion of you Singularity config yaml. There are a number of other fields, explained below, that can control the behavior and thresholds for disaster detection.

- `enabled` - set to `true` to start running disaster detection and collecting stats abotu lost tasks, lost slaves, and task lag
- `runEveryMillis` - Run the poller on this interval (defaults to every `30` seconds)

**Task Lag**
- `checkLateTasks` - Use late tasks (aka task lag) as a metric for determining if a disaster is in progress (defaults to `true`)
- `criticalAvgTaskLagMillis` - If the average time past due for all pending tasks is greater than this, a disaster is in progress (likely due to a severe lack of resources in the cluster), defaults to `4 minutes (240000)`
- `criticalOverdueTaskPortion` - If the portion of tasks taht are considered overdue is this fraction of the total running tasks in the cluster, a disaster is in progress. Defaults to `0.1` or one tenth of tasks are pending and overdue.

**Lost Slaves**
- `checkLostSlaves` - Use lost slaves as a metric for determining if a disaster is in progress. Disaster detection only counts slaves that have transitioned from `ACTIVE` to `DEAD`. Slaves that are gracefully decommissioned and removed won't trigger a disaster. (defaults to `true`)
- `criticalLostSlavePortion` - If, during the past run of the poller, this portion of the total _active_ slaves in the clsuter have transitioned from `ACTIVE` to `DEAD` a disaster is in progress. Defaults to `0.2` or one fifth of the slaves in the cluster

**Lost Tasks**
- `checkLostTasks` - Use lost tasks as a metric for determining if a disaster is in progress (defaults to `true`)
- `lostTaskReasons` - Consider status updates matching these reasons towards the lost tasks for disaster detection. This is a list of mesos `Reason` enum values (`org.apache.mesos.Protos.TaskStatus.Reason`) and defaults to `[REASON_INVALID_OFFERS, REASON_SLAVE_UNKNOWN, REASON_SLAVE_REMOVED, REASON_SLAVE_RESTARTED, REASON_MASTER_DISCONNECTED]`
- `criticalLostTaskPortion` - If this portion of the total _active_ tasks in the cluster have transitioned to `LOST` for one of the above reasons in the last run of the poller, a disaster is in progress. Defaults to `0.2`

### Disabled Actions

Singularity also supports globally disabling certain actions, which can aid in maintenance or cluster recovery after an outage. These can be added and removed manually on the `/disasters` UI page. You can also specify a list of actions that can automatically be disabled when a disaster is detected by specifying `disableActionsOnDisaster` in the `disasterDetection` portion of you Singularity config yaml. When a disaster is detected, any action specified will automatically be disabled, and will be enabled again when the disaster has cleared. If during runtime you want to stop the disaster detector from disabling actions (for example, it keeps detecting a false positive), you can disable the automated actions in the UI or `POST` to the `/api/disasters/disable` endpoint.