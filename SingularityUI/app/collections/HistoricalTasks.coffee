RequestTasks = require './RequestTasks'

class HistoricalTasks extends Mixen(RequestTasks, Teeble.ServerCollection)

    model: Backbone.Model

    comparator: undefined

    url: ->
        params =
            count: @perPage
            page: @currentPage

        "#{ window.singularity.config.apiBase }/history/request/#{ @requestId }/tasks?#{ $.param params }"

module.exports = HistoricalTasks
