Collection = require './collection'

class RequestTasks extends Collection

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/request/#{ @requestId }/tasks#{ if @active then '/active' else '' }"

    initialize: (models, { @requestId, @active }) =>

    parse: (tasks) ->
        _.each tasks, (task) ->
            task.JSONString = utils.stringJSON task
            task.id = task.taskId.id
            task.name = task.id
            task.updatedAtHuman = if task.updatedAt? then moment(task.updatedAt).from() else ''
            task.createdAtHuman = if task.createdAt? then moment(task.createdAt).from() else ''
            task.lastStatusHuman = if constants.taskStates[task.lastStatus] then constants.taskStates[task.lastStatus].label else ''
            task.isActive = if constants.taskStates[task.lastStatus] then constants.taskStates[task.lastStatus].isActive else false
            app.allTasks[task.id] = task

        tasks

module.exports = RequestTasks