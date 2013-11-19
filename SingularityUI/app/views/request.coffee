View = require './view'

RequestTasks = require '../collections/RequestTasks'

class RequestView extends View

    template: require './templates/request'

    initialize: =>
        @request = app.collections.requestsActive.get(@options.requestId)
        @requestTasks = new RequestTasks [], requestId: @options.requestId
        @requestTasks.fetch().done => @render fetchDone = true

    render: (fetchDone = false) =>
        return false unless @request

        context =
            request: @request.toJSON()
            fetchDone: fetchDone
            requestTasksActive: _.filter(@requestTasks.toJSON(), (t) -> t.isActive)
            requestTasksHistorical: _.first(_.filter(@requestTasks.toJSON(), (t) -> not t.isActive), 100)
            requestTasksScheduled: _.filter(app.collections.tasksScheduled.toJSON(), (t) => t.requestId is @options.requestId)

        @$el.html @template context

        @setupEvents()

        utils.setupSortableTables()

    setupEvents: ->
        @$el.find('.view-json').unbind('click').click (event) ->
            utils.viewJSON (utils.getAcrossCollections ['tasksActive', 'tasksScheduled'], $(event.target).data('task-id'))?.toJSON()

module.exports = RequestView