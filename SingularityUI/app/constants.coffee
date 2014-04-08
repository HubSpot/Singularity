constants =

    appRoot: '/singularitytest/'
    apiBase: 'singularitytest/v1'

    mesosLogsPort: '5051'
    mesosLogsPortHttps: '5151'

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

module.exports = constants