Collection = require './collection'

Task = require '../models/Task'

class Tasks extends Collection

    model: Task

    comparatorMap:
        active:    (one, two) ->
            one.get('taskId').startedAt - two.get('taskId').startedAt
        scheduled: (one, two) ->
            one.get('pendingTask').pendingTaskId.nextRunAt - two.get('pendingTask').pendingTaskId.nextRunAt
        cleaning:  undefined

    propertyFilterMap:
        active: ['offer.hostname', 'taskId', 'mesosTask.resources', 'rackId', 'taskRequest.request.requestType']

    url: ->
        requestFilter = if @requestId? then "/request/#{ @requestId }" else ''
        propertyString = $.param 'property': @propertyFilterMap[@state] or @addPropertyString || [], true
        "#{ config.apiRoot }/tasks/#{ @state }#{ requestFilter }?#{ propertyString }"

    initialize: (models = [], {@state, @requestId, @addPropertyString}) ->
        @comparator = @comparatorMap[@state]

module.exports = Tasks
