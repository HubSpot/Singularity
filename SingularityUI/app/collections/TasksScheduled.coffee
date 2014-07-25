Tasks = require './Tasks'

Task = require '../models/Task'

class TasksScheduled extends Tasks

    model: Task

    url: "#{ config.apiRoot }/tasks/scheduled"

    parse: (tasks) ->
        for task in tasks
            task.originalObject = _.clone task
            if not task.pendingTaskId?
                task.pendingTaskId = task.pendingTask.pendingTaskId
            task.id = @parsePendingId task.pendingTaskId
            task.requestId = task.pendingTaskId.requestId
            task.name = task.id
            task.nextRunAt = task.pendingTaskId.nextRunAt
            task.nextRunAtHuman = utils.humanTimeSoon task.nextRunAt
            task.schedule = task.request.schedule

        tasks

    parsePendingId: (pendingTaskId) ->
        "#{ pendingTaskId.requestId }-#{ pendingTaskId.nextRunAt }-#{ pendingTaskId.instanceNo }"

    comparator: 'nextRunAt'

module.exports = TasksScheduled
