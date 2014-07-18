Collection = require './collection'

class RequestTasks extends Collection

    url: => "#{ config.apiRoot }/history/request/#{ @requestId }/tasks/#{ @state }"

    comparator: -> - @get('createdAt')

    initialize: (models, { @requestId, @state, @sortColumn, @sortDirection }) => super

    parse: (tasks) ->
        _.each tasks, (task) ->
            task.JSONString = utils.stringJSON task
            task.id = task.taskId.id
            task.name = task.id
            task.deployId = task.taskId.deployId
            task.updatedAtHuman = utils.humanTimeAgo task.updatedAt
            task.startedAt = task.taskId.startedAt
            task.startedAtHuman = utils.humanTimeAgo task.startedAt
            task.lastTaskStateHuman = if constants.taskStates[task.lastTaskState] then constants.taskStates[task.lastTaskState].label else ''
            task.isActive = if constants.taskStates[task.lastTaskState] then constants.taskStates[task.lastTaskState].isActive else false
            app.allTasks[task.id] = task

        tasks

module.exports = RequestTasks
