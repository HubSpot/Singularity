RequestTasks = require './RequestTasks'

class HistoricalTasks extends Mixen(RequestTasks, Teeble.ServerCollection)

    model: Backbone.Model

    comparator: (task0, task1) =>
        -(task0.get("startedAt") - task1.get("startedAt"))

    url: ->
        params =
            count: @perPage
            page: @currentPage

        "#{ config.apiRoot }/history/request/#{ @requestId }/tasks?#{ $.param params }"

module.exports = HistoricalTasks
