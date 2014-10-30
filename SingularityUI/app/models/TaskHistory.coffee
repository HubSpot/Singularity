Model = require './model'

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
        
        taskHistory

    parseResources: (task) ->
        cpus: _.find(task.mesosTask.resources, (resource) -> resource.name is 'cpus')?.scalar?.value ? ''
        memoryMb: _.find(task.mesosTask.resources, (resource) -> resource.name is 'mem')?.scalar?.value ? ''

module.exports = TaskHistory
