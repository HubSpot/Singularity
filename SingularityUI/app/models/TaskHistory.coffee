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


    parseResources: (task) ->
        cpus: _.find(task.mesosTask.resources, (resource) -> resource.name is 'cpus')?.scalar?.value ? ''
        memoryMb: _.find(task.mesosTask.resources, (resource) -> resource.name is 'mem')?.scalar?.value ? ''

module.exports = TaskHistory
