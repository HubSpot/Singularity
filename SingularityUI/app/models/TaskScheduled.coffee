Model = require './model'

class TaskScheduled extends Model

    parsePendingId: (pendingTaskId) ->
        "#{ pendingTaskId.requestId }-#{ pendingTaskId.nextRunAt }-#{ pendingTaskId.instanceNo }"

    parse: (task) =>
        task.JSONString = utils.stringJSON task
        if not task.pendingTaskId?
            task.pendingTaskId = task.pendingTask.pendingTaskId
        task.id = @parsePendingId task.pendingTaskId
        task.requestId = task.pendingTaskId.requestId
        task.name = task.id
        task.nextRunAt = task.pendingTaskId.nextRunAt
        task.nextRunAtHuman = utils.humanTimeSoon task.nextRunAt
        if task.request?
            task.schedule = task.request.schedule
        app.allTasks[task.id] = task

        task

    run: ->
        $.ajax
            url: "#{ config.apiRoot }/requests/request/#{ @get('requestId') }/run"
            type: 'POST'
            contentType: 'application/json'

module.exports = TaskScheduled