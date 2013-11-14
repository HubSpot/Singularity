Collection = require './collection'

Tasks = require './Tasks'

class TasksActive extends Tasks

    url: "http://#{env.SINGULARITY_BASE}/#{constants.api_base}/tasks/active"

    parse: (tasks) ->
        _.each tasks, (task, i) =>
            task.id = task.task.taskId.value
            task.name = task.task.name
            task.resourcesCount = task.task.resources.length
            task.startedAt = task.taskId.startedAt
            task.startedAtHuman = moment(task.taskId.startedAt).from()
            task.JSONString = utils.stringJSON task
            tasks[i] = task

        tasks

    comparator: 'name'

module.exports = TasksActive