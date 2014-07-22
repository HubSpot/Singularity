RequestTasks = require './RequestTasks'
PaginableCollection = require './PaginableCollection'

class HistoricalTasks extends PaginableCollection

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/tasks"

    initialize: (models, {@requestId}) ->

    comparator: (task0, task1) =>
        -(task0.get("updatedAt") - task1.get("updatedAt"))

    parse: (tasks) ->
        for task in tasks
            task.JSONString = utils.stringJSON task
            task.id = task.taskId.id
            task.name = task.id
            task.deployId = task.taskId.deployId
            task.updatedAtHuman = utils.humanTimeAgo task.updatedAt
            task.startedAt = task.taskId.startedAt
            task.startedAtHuman = utils.humanTimeAgo task.startedAt
            task.lastTaskStateHuman = if constants.taskStates[task.lastTaskState] then constants.taskStates[task.lastTaskState].label else ''
            task.isActive = if constants.taskStates[task.lastTaskState] then constants.taskStates[task.lastTaskState].isActive else false

        tasks

module.exports = HistoricalTasks
