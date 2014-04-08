View = require './view'

RequestHistoricalTasksTableView = require './requestHistoricalTasksTable'

class RequestHistoricalTasksView extends View

    template: require './templates/requestHistoricalTasks'

    refresh: -> @

    render: ->
        context =
            request:
                id: @options.requestId
                name: utils.getRequestNameFromID @options.requestId
            requestNameStringLengthTens: Math.floor(@options.requestId.length / 10) * 10

        @$el.html @template context
        @renderHistoricalTasksPaginated()

        @

    renderHistoricalTasksPaginated: ->
        requestHistoricalTasksTable = new RequestHistoricalTasksTableView
            requestId: @options.requestId
            count: 200

        @$el.find('.historical-tasks-paginated').html requestHistoricalTasksTable.render().$el

module.exports = RequestHistoricalTasksView