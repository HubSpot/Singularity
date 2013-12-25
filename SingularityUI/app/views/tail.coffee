View = require './view'

TaskHistory = require '../models/TaskHistory'
TailerView = require './tailer'

class TailView extends View
    template: require './templates/tail'

    initialize: ({@taskId, @path}) ->
        @deferredSetup = Q.defer()

        @taskHistory = new TaskHistory [], {@taskId}
        ajaxPromise = @taskHistory.fetch()
        
        ajaxPromise.done =>
            @tailer = new TailerView
                taskHistory: @taskHistory
                path: @path

            @tailer.setup()

            @deferredSetup.resolve(@tailer)

        ajaxPromise.fail (jqXHR, status, error) =>
            @deferredSetup.reject(error)

    setup: =>
        return @deferredSetup.promise

    render: =>
        @$el.html @template {@taskHistory, @path}

        # only render the tailer if it's initialized...
        if @deferredSetup.promise.isFulfilled()
            @$el.find('div.tail-container').html @tailer.$el

        @

module.exports = TailView