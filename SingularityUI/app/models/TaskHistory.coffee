Model = require './model'

class TaskHistory extends Model

    url: -> "#{ config.apiRoot }/history/task/#{ @taskId }"

    initialize: ({ @taskId }) ->

    parse: (taskHistory) ->
        taskHistory.task.JSONString = utils.stringJSON taskHistory

        taskHistory.task.id = taskHistory.task.taskId.id
        taskHistory.task.name = taskHistory.task.mesosTask.name
        taskHistory.task.resources = @parseResources taskHistory.task
        taskHistory.task.memoryHuman = if taskHistory.task.resources?.memoryMb? then "#{ taskHistory.task.resources.memoryMb }Mb" else ''
        taskHistory.task.host = taskHistory.task.offer.hostname?.split('.')[0]
        taskHistory.task.startedAt = taskHistory.task.taskId.startedAt
        taskHistory.task.startedAtHuman = utils.humanTimeAgo taskHistory.task.taskId.startedAt
        taskHistory.task.rack = taskHistory.task.taskId.rackId
        taskHistory.task.isStopped = false

        _.each taskHistory.taskUpdates, (taskUpdate, i) =>
            taskUpdate.taskStateHuman = if constants.taskStates[taskUpdate.taskState] then constants.taskStates[taskUpdate.taskState].label else ''
            taskUpdate.statusMessage = taskUpdate.statusMessage ? ''
            taskUpdate.timestampHuman = utils.humanTimeAgo taskUpdate.timestamp

            if taskUpdate.taskState in constants.inactiveTaskStates
                taskHistory.task.isStopped = true

        _.sortBy taskHistory.taskUpdates, (t) -> t.timestamp

        # Construct mesos logs link
        taskHistory.mesosMasterLogsLink = "http://#{ app.state.get('masterLogsDomain') }/#/slaves/#{ taskHistory.task.offer.slaveId.value }/browse?path=#{ taskHistory.directory }"

        app.allTasks[taskHistory.task.id] = taskHistory.task

        taskHistory

    parseResources: (task) ->
        cpus: _.find(task.mesosTask.resources, (resource) -> resource.name is 'cpus')?.scalar?.value ? ''
        memoryMb: _.find(task.mesosTask.resources, (resource) -> resource.name is 'mem')?.scalar?.value ? ''

module.exports = TaskHistory