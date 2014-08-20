Model = require './model'

# This model hits up the history API and gets us the record for
# an old (or current) Task
class TaskHistory extends Model

    url: -> "#{ config.apiRoot }/history/task/#{ @taskId }"

    initialize: ({ @taskId }) ->

    parse: (taskHistory) ->
        _.sortBy taskHistory.taskUpdates, (t) -> t.timestamp
        taskHistory.task?.mesosTask?.executor?.command?.environment?.variables = _.sortBy taskHistory.task.mesosTask.executor.command.environment.variables, "name"

        taskHistory

    parseResources: (task) ->
        cpus: _.find(task.mesosTask.resources, (resource) -> resource.name is 'cpus')?.scalar?.value ? ''
        memoryMb: _.find(task.mesosTask.resources, (resource) -> resource.name is 'mem')?.scalar?.value ? ''

module.exports = TaskHistory
