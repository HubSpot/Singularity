Controller = require './Controller'

LogLines = require '../collections/LogLines'
TaskHistory = require '../models/TaskHistory'
AjaxError = require '../models/AjaxError'

TailView = require '../views/tail'

class TailController extends Controller

    initialize: ({@taskId, @path, @offset}) ->
        @title 'Tail of ' + @path

        @models.ajaxError = new AjaxError
        @collections.logLines = new LogLines [], {@taskId, @path, ajaxError: @models.ajaxError}
        @models.taskHistory = new TaskHistory {@taskId}

        @setView new TailView _.extend {@taskId, @path, @offset},
            collection: @collections.logLines
            model: @models.activeTasks
            ajaxError: @models.ajaxError

        app.showView @view

        if @offset?
            $.when( @collections.logLines.fetchOffset(@offset), @refresh() ).then => @view.afterInitialOffsetData()
        else
            $.when( @collections.logLines.fetchInitialData(), @refresh() ).then => @view.afterInitialData()

    refresh: ->
        @models.taskHistory.fetch()


module.exports = TailController
