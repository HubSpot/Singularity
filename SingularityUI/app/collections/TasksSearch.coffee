Tasks = require './Tasks'

class TasksSearch extends Tasks

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/tasks/search?#{ utils.paramsString @params }requestIdLike=#{ @query }"

    initialize: (models, { @query, @params }) =>

    parse: (tasks) ->
        _.each tasks, (task, i) =>
            task.JSONString = utils.stringJSON task
            task.id = task.taskId.id
            task.requestId = task.taskId.requestId
            task.name = task.id
            task.statusUpdateHuman = if constants.taskStates[task.lastStatus] then constants.taskStates[task.lastStatus].label else ''
            task.startedAt = task.taskId.startedAt
            task.startedAtHuman = moment(task.taskId.startedAt).from()
            task.updatedAtHuman = if task.updatedAt? then moment(task.updatedAt).from() else ''
            task.createdAtHuman = if task.createdAt? then moment(task.createdAt).from() else ''
            task.host = task.taskId.host
            task.rack = task.taskId.rackId
            app.allTasks[task.id] = task

        tasks

module.exports = TasksSearch