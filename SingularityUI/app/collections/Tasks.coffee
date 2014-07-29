Collection = require './collection'

class Tasks extends Collection

    comparatorMap:
        active:    'startedAt'
        scheduled: 'nextRunAt'
        cleaning:  ''

    propertyFilterMap:
        active: ['offer.hostname', 'taskId', 'mesosTask.resources']

    comparator: -> @comparatorMap[@state]

    url: ->
        propertyString = $.param 'property': @propertyFilterMap[@state] or [], true
        "#{ config.apiRoot }/tasks/#{ @state }?#{ propertyString }"

    initialize: (models = [], {@state}) ->

    parse: (tasks) ->
        # This will get replaced very soon. I know it's hideous, it'll go away
        if @state is 'active'
            for task in tasks
                task.host = task.offer.hostname?.split('.')[0]
                task.JSONString = utils.stringJSON task
                task.id = task.taskId.id
                task.requestId = task.taskId.requestId
                task.name = task.requestId
                task.resources = @parseResources task
                task.cpus = task.resources.cpus
                task.memoryMb = task.resources.memoryMb
                task.memoryHuman = if task.resources?.memoryMb? then "#{ task.resources.memoryMb }Mb" else ''
                task.startedAt = task.taskId.startedAt
                task.startedAtHuman = utils.humanTimeAgo task.taskId.startedAt
                task.rack = task.taskId.rackId
        else if @state is 'scheduled'
            for task in tasks
                task.JSONString = utils.stringJSON task
                if not task.pendingTaskId?
                    task.pendingTaskId = task.pendingTask.pendingTaskId
                task.id = @parsePendingId task.pendingTaskId
                task.requestId = task.pendingTaskId.requestId
                task.name = task.id
                task.nextRunAt = task.pendingTaskId.nextRunAt
                task.nextRunAtHuman = utils.humanTimeSoon task.nextRunAt
                task.schedule = task.request.schedule
        else if @state is 'cleaning'
            for task in tasks
                task.JSONString = utils.stringJSON task
                task.id = task.taskId.id
                task.name = task.id
                task.timestampHuman = moment(task.timestamp).fromNow()
                task.cleanupTypeHuman = constants.taskCleanupTypes[task.cleanupType] or ''

        tasks

    parseResources: (task) ->
        cpus: _.find(task.mesosTask.resources, (resource) -> resource.name is 'cpus')?.scalar?.value ? ''
        memoryMb: _.find(task.mesosTask.resources, (resource) -> resource.name is 'mem')?.scalar?.value ? ''

    parsePendingId: (pendingTaskId) ->
        "#{ pendingTaskId.requestId }-#{ pendingTaskId.nextRunAt }-#{ pendingTaskId.instanceNo }"

module.exports = Tasks
