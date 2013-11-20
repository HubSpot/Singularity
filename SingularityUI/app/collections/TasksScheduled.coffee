Tasks = require './Tasks'

class TasksScheduled extends Tasks

    url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/tasks/scheduled"

    parse: (tasks) ->
        _.each tasks, (task, i) =>
            task.JSONString = utils.stringJSON task
            task.id = @parsePendingId task.pendingTaskId
            task.requestId = task.pendingTaskId.requestId
            task.name = task.id
            task.nextRunAt = task.pendingTaskId.nextRunAt
            task.nextRunAtHuman = moment(task.nextRunAt).fromNow()
            task.schedule = task.request.schedule
            task.JSONString = utils.stringJSON task
            tasks[i] = task
            app.allTasks[task.id] = task

        tasks

    parsePendingId: (pendingTaskId) ->
        "#{ pendingTaskId.requestId }-#{ pendingTaskId.nextRunAt }-#{ pendingTaskId.instanceNo }"

    comparator: 'nextRunAt'

module.exports = TasksScheduled