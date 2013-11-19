Tasks = require './Tasks'

class TasksActive extends Tasks

    url: "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/tasks/active"

    parse: (tasks) ->
        _.each tasks, (task, i) =>
            task.JSONString = utils.stringJSON task
            task.id = task.taskId.id
            task.name = task.mesosTask.name
            task.resources = @parseResources task
            task.memoryHuman = if task.resources?.memoryMb? then "#{ task.resources.memoryMb }Mb" else ''
            task.host = task.offer.hostname?.split('.')[0]
            task.startedAt = task.taskId.startedAt
            task.startedAtHuman = moment(task.taskId.startedAt).from()
            task.rack = task.taskId.rackId
            tasks[i] = task
            app.allTasks[task.id] = task

        tasks

    parseResources: (task) ->
        cpus: _.find(task.mesosTask.resources, (resource) -> resource.name is 'cpus')?.scalar?.value ? ''
        memoryMb: _.find(task.mesosTask.resources, (resource) -> resource.name is 'mem')?.scalar?.value ? ''

    comparator: 'startedAt'

module.exports = TasksActive