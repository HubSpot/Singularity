Collection = require './collection'

Task = require '../models/Task'

class Tasks extends Collection

    model: Task

    comparatorMap:
        active:    (one, two) ->
            two.get('taskId').startedAt - one.get('taskId')?.startedAt
        scheduled: (one, two) ->
            one.get('pendingTask').pendingTaskId.nextRunAt - two.get('pendingTask').pendingTaskId.nextRunAt
        cleaning:  undefined

    propertyFilterMap:
        active: ['offer.hostname', 'taskId', 'mesosTask.resources']

    url: ->
        requestFilter = if @requestId? then "/request/#{ @requestId }" else ''
        propertyString = $.param 'property': @propertyFilterMap[@state] or [], true
        "#{ config.apiRoot }/tasks/#{ @state }#{ requestFilter }?#{ propertyString }"

    setState: (state) ->
        @state = state
        @comparator = @comparatorMap[@state]

    initialize: (models = [], {@state, @requestId}) ->
        @comparator = @comparatorMap[@state]

module.exports = Tasks
