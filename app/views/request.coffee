View = require './view'

RequestTasks = require '../models/RequestTasks'

class RequestView extends View

    template: require './templates/request'

    initialize: =>
        @request = app.collections.requests.get(@options.requestId)
        @requestTasks = new RequestTasks()
        @requestTasks.fetch().done => @render()

    render: =>
        return false unless @request

        context =
            request: @request.toJSON()
            requestTasks: @requestTasks.toJSON()

        @$el.html @template context

module.exports = RequestView