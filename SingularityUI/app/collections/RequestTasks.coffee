Collection = require './collection'

class RequestTasks extends Collection

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/request/#{ @requestId }/tasks#{ if @active then '/active' else '' }"

    comparator: -> - @get('createdAt')

    initialize: (models, { @requestId, @active, @sortColumn, @sortDirection }) =>
        super

    parse: (tasks) ->
        _.each tasks, (task) ->
            task.JSONString = utils.stringJSON task
            task.id = task.taskId.id
            task.name = task.id
            task.updatedAtHuman = utils.humanTimeAgo task.updatedAt
            task.createdAtHuman = utils.humanTimeAgotask.createdAt
            task.lastStatusHuman = if constants.taskStates[task.lastStatus] then constants.taskStates[task.lastStatus].label else ''
            task.isActive = if constants.taskStates[task.lastStatus] then constants.taskStates[task.lastStatus].isActive else false
            app.allTasks[task.id] = task

        tasks

module.exports = RequestTasks
