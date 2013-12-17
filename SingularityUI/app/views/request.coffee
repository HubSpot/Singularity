View = require './view'

RequestTasks = require '../collections/RequestTasks'

class RequestView extends View

    template: require './templates/request'

    initialize: =>
        @request = app.allRequests[@options.requestId]

        @requestTasksActive = new RequestTasks [], { requestId: @options.requestId, active: true }
        @requestTasksActive.fetch().done =>
            @fetchDoneActive = true
            @render()

        @requestTasksHistorical = new RequestTasks [], { requestId: @options.requestId, active: false }
        @requestTasksHistorical.fetch().done =>
            @fetchDoneHistorical = true
            @render()

    render: =>
        if not @request
            vex.dialog.alert("<p>Could not open a request by that ID.</p><pre>#{ @options.requestId }</pre>")
            return

        context =
            request: @request

            fetchDoneActive: @fetchDoneActive
            fetchDoneHistorical: @fetchDoneHistorical

            requestTasksActive: _.pluck(@requestTasksActive.models, 'attributes')
            requestTasksHistorical: _.filter(_.pluck(@requestTasksHistorical.models, 'attributes'), (t) => not t.isActive)

            requestTasksScheduled: _.filter(_.pluck(app.collections.tasksScheduled.models, 'attributes'), (t) => t.requestId is @options.requestId)

        @$el.html @template context

        @setupEvents()

        utils.setupSortableTables()

    setupEvents: ->
        @$el.find('.view-json').unbind('click').click (event) ->
            utils.viewJSON 'task', $(event.target).data('task-id')

        @$el.find('.view-object-json').unbind('click').click (event) ->
            utils.viewJSON 'request', $(event.target).data('request-id')

module.exports = RequestView