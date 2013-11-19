constants =

    app_name: 'singularity'
    config_server_base: '/singularity'
    api_base: 'singularity/v1'

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

    driverStates:
        DRIVER_ABORTED: 'aborted'
        DRIVER_NOT_STARTED: 'not started'
        DRIVER_RUNNING: 'running'
        DRIVER_STOPPED: 'stopped'

module.exports = constants