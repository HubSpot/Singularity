View = require './view'

RequestTasks = require '../models/RequestTasks'

class RequestView extends View

    template: require './templates/request'

    initialize: =>
        @request = app.collections.requests.get(@options.requestId)
        @requestTasks = new RequestTasks requestId: @options.requestId
        @requestTasks.fetch().done => @render()

    render: =>
        return false unless @request

        context =
            request: @request.toJSON()
            requestTasks: @requestTasks.toJSON()

        @$el.html @template context

        @setupEvents()

    setupEvents: ->
        @$el.find('.view-json').unbind('click').click (event) ->
            utils.viewJSON (utils.getAcrossCollections [app.collections.tasksActive, app.collections.tasksScheduled], $(event.target).data('task-id'))?.toJSON()

module.exports = RequestView