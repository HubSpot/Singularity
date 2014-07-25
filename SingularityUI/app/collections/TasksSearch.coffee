Tasks = require './Tasks'

class TasksSearch extends Tasks

    url: => "#{ config.apiRoot }/history/tasks/search?count=6&#{ $.param @params }&requestIdLike=#{ @query }"

    initialize: (models, { @query, @params }) =>

    parse: (tasks) ->
        for task in tasks
            task.originalObject = _.clone task
            task.id = task.taskId.id
            task.requestId = task.taskId.requestId
            task.name = task.id
            task.statusUpdateHuman = if constants.taskStates[task.lastStatus] then constants.taskStates[task.lastStatus].label else ''
            task.startedAt = task.taskId.startedAt
            task.startedAtHuman = utils.humanTimeAgo task.taskId.startedAt
            task.updatedAtHuman = utils.humanTimeAgo task.updatedAt
            task.createdAtHuman = utils.humanTimeAgo task.createdAt
            task.host = task.taskId.host
            task.rack = task.taskId.rackId

        tasks

    comparator: 'startedAt'

module.exports = TasksSearch
