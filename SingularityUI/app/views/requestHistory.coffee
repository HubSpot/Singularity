View = require './view'

RequestHistoryTableView = require './requestHistoryTable'

class RequestHistoryView extends View

    template: require './templates/requestHistory'

    refresh: -> @

    render: ->
        context =
            request:
                id: @options.requestId
                name: utils.getRequestNameFromID @options.requestId
            requestNameStringLengthTens: Math.floor(@options.requestId.length / 10) * 10

        @$el.html @template context
        @renderHistoryPaginated()

        @

    renderHistoryPaginated: ->
        requestHistoryTable = new RequestHistoryTableView
            requestId: @options.requestId
            count: 200

        @$el.find('.history-paginated').html requestHistoryTable.render().$el

module.exports = RequestHistoryView
