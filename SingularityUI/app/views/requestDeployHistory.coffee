View = require './view'

RequestDeployHistoryTableView = require './requestDeployHistoryTable'

class RequestDeployHistoryView extends View

    template: require './templates/requestDeployHistory'

    refresh: -> @

    render: ->
        context =
            request:
                id: @options.requestId
                name: utils.getRequestNameFromID @options.requestId
            requestNameStringLengthTens: Math.floor(@options.requestId.length / 10) * 10

        @$el.html @template context
        @renderDeployHistoryPaginated()

        @

    renderDeployHistoryPaginated: ->
        requestDeployHistoryTable = new RequestDeployHistoryTableView
            requestId: @options.requestId
            count: 200

        @$el.find('.deploy-history-paginated').html requestDeployHistoryTable.render().$el

module.exports = RequestDeployHistoryView
