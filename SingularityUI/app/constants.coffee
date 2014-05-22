constants =
    taskStates:
        TASK_RUNNING:
            isActive: true
            label: 'Running'
        TASK_STAGING:
            isActive: true
            label: 'Staging'
        TASK_STARTING:
            isActive: true
            label: 'Starting'
        TASK_FAILED:
            isActive: false
            label: 'Failed'
        TASK_FINISHED:
            isActive: false
            label: 'Finished'
        TASK_KILLED:
            isActive: false
            label: 'Killed'
        TASK_LOST:
            isActive: false
            label: 'Lost'
        TASK_CLEANING:
            isActive: false
            label: 'Cleaning' # Trying to be killed?
        TASK_LOST_WHILE_DOWN:
            isActive: false
            label: 'Lost while down'

    requestStates:
        CREATED: 'Created'
        UPDATED: 'Updated'
        DELETED: 'Deleted'
        PAUSED: 'Paused'
        UNPAUSED: 'Unpaused'

    requestCleanupTypes:
        DELETING: 'Deleting'
        PAUSING: 'Pausing'

    taskCleanupTypes:
        BOUNCING: 'Bouncing'
        USER_REQUESTED: 'User requested'
        DECOMISSIONING: 'Decomissioning'

    driverStates:
        DRIVER_ABORTED: 'aborted'
        DRIVER_NOT_STARTED: 'not started'
        DRIVER_RUNNING: 'running'
        DRIVER_STOPPED: 'stopped'

    deployStates:
        SUCCEEDED: 'Succeeded'
        FAILED_INTERNAL_STATE: 'Failed internal state'
        WAITING: 'Waiting'
        OVERDUE: 'Overdue'
        FAILED: 'Failed'
        CANCELED: 'Canceled'

constants.inactiveTaskStates = []
for state, stateObj of constants.taskStates
    if stateObj.isActive is false
        constants.inactiveTaskStates.push state

module.exports = constants