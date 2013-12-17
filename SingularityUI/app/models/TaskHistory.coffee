Model = require './model'

class TaskHistory extends Model

    url: -> "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/task/#{ @taskId }"

    initialize: (models, { @taskId }) =>

    parse: (taskHistory) ->
        taskHistory.task.JSONString = utils.stringJSON taskHistory

        taskHistory.task.id = taskHistory.task.taskId.id
        taskHistory.task.name = taskHistory.task.mesosTask.name
        taskHistory.task.resources = @parseResources taskHistory.task
        taskHistory.task.memoryHuman = if taskHistory.task.resources?.memoryMb? then "#{ taskHistory.task.resources.memoryMb }Mb" else ''
        taskHistory.task.host = taskHistory.task.offer.hostname?.split('.')[0]
        taskHistory.task.startedAt = taskHistory.task.taskId.startedAt
        taskHistory.task.startedAtHuman = moment(taskHistory.task.taskId.startedAt).from()
        taskHistory.task.rack = taskHistory.task.taskId.rackId

        _.each taskHistory.taskUpdates, (taskUpdate, i) =>
            taskUpdate.statusUpdateHuman = if constants.taskStates[taskUpdate.statusUpdate] then constants.taskStates[taskUpdate.statusUpdate].label else ''
            taskUpdate.statusMessage = taskUpdate.statusMessage ? 'No status message available'
            taskUpdate.timestampHuman = moment(taskUpdate.timestamp).from()

        # Construct mesos logs link
        taskHistory.mesosMasterLogsLink = "http://#{ app.state.get('masterLogsDomain') }/#/slaves/#{ taskHistory.task.offer.slaveId.value }/browse?path=#{ taskHistory.directory }"

        app.allTasks[taskHistory.task.id] = taskHistory.task

        taskHistory

    parseResources: (task) ->
        cpus: _.find(task.mesosTask.resources, (resource) -> resource.name is 'cpus')?.scalar?.value ? ''
        memoryMb: _.find(task.mesosTask.resources, (resource) -> resource.name is 'mem')?.scalar?.value ? ''

module.exports = TaskHistory