Collection = require './collection'

Tasks = require './Tasks'

class TasksScheduled extends Tasks

    url: "http://#{env.SINGULARITY_BASE}/#{constants.api_base}/task/active"

    parse: (tasks) ->
        _.each tasks, (task, i) =>
            task.id = @parsePendingId task.pendingTaskId
            task.name = task.task.name
            task.nextRunAt = task.pendingTaskId.nextRunAt
            task.nextRunAtHuman = moment(task.nextRunAt).fromNow()
            task.schedule = task.request.schedule
            tasks[i] = task

        tasks

    parsePendingId: (pendingTaskId) ->
        "#{ pendingTaskId.name }-#{ pendingTaskId.nextRunAt }-#{ pendingTaskId.instanceNo }"

    comparator: 'nextRunAt'

module.exports = TasksScheduled