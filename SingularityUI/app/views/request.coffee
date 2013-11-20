View = require './view'

RequestTasks = require '../collections/RequestTasks'

class RequestView extends View

    template: require './templates/request'

    initialize: =>
        @request = app.allRequests[@options.requestId]

        @requestTasks = new RequestTasks [], requestId: @options.requestId
        @requestTasks.fetch().done =>
            @fetchDone = true
            @render()

    render: =>
        if not @request
            vex.dialog.alert("<p>Could not open a request by that ID.</p><pre>#{ @options.requestId }</pre>")
            return

        context =
            request: @request
            fetchDone: @fetchDone
            requestTasksActive: _.filter(@requestTasks.toJSON(), (t) -> t.isActive)
            requestTasksHistorical: _.first(_.filter(@requestTasks.toJSON(), (t) -> not t.isActive), 100)
            requestTasksScheduled: _.filter(app.collections.tasksScheduled.toJSON(), (t) => t.requestId is @options.requestId)

        @$el.html @template context

        @setupEvents()

        utils.setupSortableTables()

    setupEvents: ->
        @$el.find('.view-json').unbind('click').click (event) ->
            utils.viewJSON 'task', $(event.target).data('task-id')

module.exports = RequestView