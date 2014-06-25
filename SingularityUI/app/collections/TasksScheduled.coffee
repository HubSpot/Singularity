Tasks = require './Tasks'
TaskScheduled = require '../models/TaskScheduled'

class TasksScheduled extends Tasks

    model: TaskScheduled

    url: "#{ config.apiRoot }/tasks/scheduled"

    parse: (tasks) ->
        _.each tasks, (task, i) =>
            task.JSONString = utils.stringJSON task
            if not task.pendingTaskId?
                task.pendingTaskId = task.pendingTask.pendingTaskId
            task.id = @parsePendingId task.pendingTaskId
            task.requestId = task.pendingTaskId.requestId
            task.name = task.id
            task.nextRunAt = task.pendingTaskId.nextRunAt
            task.nextRunAtHuman = utils.humanTimeSoon task.nextRunAt
            task.schedule = task.request.schedule
            tasks[i] = task
            app.allTasks[task.id] = task

        tasks

    parsePendingId: (pendingTaskId) ->
        "#{ pendingTaskId.requestId }-#{ pendingTaskId.nextRunAt }-#{ pendingTaskId.instanceNo }"

    comparator: 'nextRunAt'

module.exports = TasksScheduled