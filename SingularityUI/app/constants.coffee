constants =

    appName: 'singularity'
    apiBase: 'singularity/v1'

    mesosLogsPort: '5051'

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

    requestCleanupType:
        DELETING: 'Deleting'
        PAUSING: 'Pausing'

    taskCleanupType:
        USER_REQUESTED:
            label: 'User requested'
        DECOMISSIONING:
            label: 'Decomissioning'

    driverStates:
        DRIVER_ABORTED: 'aborted'
        DRIVER_NOT_STARTED: 'not started'
        DRIVER_RUNNING: 'running'
        DRIVER_STOPPED: 'stopped'

module.exports = constants