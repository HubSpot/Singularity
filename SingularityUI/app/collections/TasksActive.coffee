Tasks = require './Tasks'

class TasksActive extends Tasks

    url: "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/tasks/active"

    parse: (tasks) ->
        _.each tasks, (task, i) =>
            task.id = task.taskId.id
            task.name = task.mesosTask.name
            task.resources = task.taskRequest.request.resources
            task.memoryHuman = if task.resources?.memoryMb? then "#{ task.resources.memoryMb }Mb" else ''
            task.host = task.offer.hostname?.split('.')[0]
            task.startedAt = task.taskId.startedAt
            task.startedAtHuman = moment(task.taskId.startedAt).from()
            task.JSONString = utils.stringJSON task
            tasks[i] = task

        tasks

    comparator: 'startedAt'

module.exports = TasksActive