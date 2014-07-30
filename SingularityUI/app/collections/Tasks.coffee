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
        active: ['offer.hostname', 'taskId', 'mesosTask.resources']

    url: ->
        propertyString = $.param 'property': @propertyFilterMap[@state] or [], true
        "#{ config.apiRoot }/tasks/#{ @state }?#{ propertyString }"

    initialize: (models = [], {@state}) ->
        @comparator = @comparatorMap[@state]

module.exports = Tasks
