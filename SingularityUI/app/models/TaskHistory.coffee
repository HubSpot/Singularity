Model = require './model'

Slave = require './Slave'

# This model hits up the history API and gets us the record for
# an old (or current) Task
class TaskHistory extends Model

    url: -> "#{ config.apiRoot }/history/task/#{ @taskId }"

    initialize: ({ @taskId }) ->

    parse: (taskHistory) ->
        _.sortBy taskHistory.taskUpdates, (t) -> t.timestamp
        taskHistory.task?.mesosTask?.executor?.command?.environment?.variables = _.sortBy taskHistory.task.mesosTask.executor.command.environment.variables, "name"

        ports = []

        if taskHistory.task?.taskRequest?.deploy?.resources?.numPorts > 0
          for resource in taskHistory.task.mesosTask.resources
            if resource.name == 'ports'
              for range in resource.ranges.range
                for port in [range.begin...range.end + 1]
                  ports.push(port)

        taskHistory.ports = ports

        isStillRunning = true

        for taskUpdate in taskHistory.taskUpdates
          if taskUpdate.taskState in ["TASK_KILLED", "TASK_LOST", "TASK_FAILED", "TASK_FINISHED"]
            isStillRunning = false
            break

        taskHistory.isStillRunning = isStillRunning
        if taskHistory.task.offer?
            taskHistory.slaveId = taskHistory.task.offer.slaveId.value
            taskHistory.slave = new Slave id: taskHistory.slaveId
            taskHistory.slave.fetch
                async: false
                error: =>
                    app.caughtError()
            decommission_states = ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'DECOMISSIONING', 'DECOMISSIONED', 'STARTING_DECOMISSION']
            if taskHistory.slave.attributes.state in decommission_states
                taskHistory.decommissioning = true
            else if taskHistory.slave.attributes.state is not 'ACTIVE'
                taskHistory.slaveMissing = true
        taskHistory
    
    setCurrentState: ->
        isCleaning = _.last( @get 'taskUpdates' ).taskState is 'TASK_CLEANING'
        @set 'isCleaning', isCleaning

    setCleanupMessage: (cleanupType) ->
        cleanupMessages =
            USER_REQUESTED        : "User clicked the Kill Task button in the UI."
            DECOMISSIONING        : "Slave the task is running on is decomissioning."
            SCALING_DOWN          : "Parent request's instance # was decreased, killing this task as a result."
            BOUNCING              : "Parent request is bouncing, can't kill this task until its replacement task(s) are healthy."
            DEPLOY_FAILED         : "This task was part of a new deploy that failed for some reason, so this task isn't necessary anymore."
            NEW_DEPLOY_SUCCEEDED  : "A deploy completed successfully, and this task belongs to the old deploy."
            DEPLOY_CANCELED       : "This task was part of a deploy that was cancelled, so this task isn't necessary anymore."
            UNHEALTHY_NEW_TASK    : "This task is part of a pending deploy, and it didn't become healthy -- kill this task and try again with a new one."
            OVERDUE_NEW_TASK      : "This task took too long to become healthy, so we're killing it."

        @set 'isInCleanup', true
        @set 'cleanupMessage', cleanupMessages[cleanupType]

    parseResources: (task) ->
        cpus: _.find(task.mesosTask.resources, (resource) -> resource.name is 'cpus')?.scalar?.value ? ''
        memoryMb: _.find(task.mesosTask.resources, (resource) -> resource.name is 'mem')?.scalar?.value ? ''

module.exports = TaskHistory
