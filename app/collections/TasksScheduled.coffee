Collection = require './collection'

Tasks = require './Tasks'

class TasksScheduled extends Tasks

    url: "http://#{ env.SINGULARITY_BASE }/#{ constants.api_base }/tasks/scheduled"

    parse: (tasks) ->
        _.each tasks, (task, i) =>
            task.id = @parsePendingId task.pendingTaskId
            task.name = task.id
            task.nextRunAt = task.pendingTaskId.nextRunAt
            task.nextRunAtHuman = moment(task.nextRunAt).fromNow()
            task.schedule = task.request.schedule
            task.JSONString = utils.stringJSON task
            tasks[i] = task

        tasks

    parsePendingId: (pendingTaskId) ->
        "#{ pendingTaskId.requestId }-#{ pendingTaskId.nextRunAt }-#{ pendingTaskId.instanceNo }"

    comparator: 'nextRunAt'

module.exports = TasksScheduled