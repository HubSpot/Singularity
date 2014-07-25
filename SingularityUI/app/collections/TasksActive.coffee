Tasks = require './Tasks'
Task = require '../models/Task'

class TasksActive extends Tasks

    model: Task

    propertyFilter = ['offer.hostname', 'taskId', 'mesosTask.resources']

    url: ->
        propertyString = $.param 'property': @propertyFilter or [], true
        "#{ config.apiRoot }/tasks/active#{ propertyString }"

    parse: (tasks) ->
        for task in tasks
            task.originalObject = _.clone task
            task.id = task.taskId.id
            task.requestId = task.taskId.requestId
            task.name = task.requestId
            task.resources = @parseResources task
            task.cpus = task.resources.cpus
            task.memoryMb = task.resources.memoryMb
            task.memoryHuman = if task.resources?.memoryMb? then "#{ task.resources.memoryMb }Mb" else ''
            task.host = task.offer.hostname?.split('.')[0]
            task.startedAt = task.taskId.startedAt
            task.startedAtHuman = utils.humanTimeAgo task.taskId.startedAt
            task.rack = task.taskId.rackId

        tasks

    parseResources: (task) ->
        cpus: _.find(task.mesosTask.resources, (resource) -> resource.name is 'cpus')?.scalar?.value ? ''
        memoryMb: _.find(task.mesosTask.resources, (resource) -> resource.name is 'mem')?.scalar?.value ? ''

    comparator: 'startedAt'

module.exports = TasksActive