Controller = require './Controller'

LogLines = require '../collections/LogLines'
TaskHistory = require '../models/TaskHistory'

TailView = require '../views/tail'

class TailController extends Controller

    initialize: ({@taskId, @path, @offset}) ->
        @collections.logLines = new LogLines [], {@taskId, @path}
        @models.taskHistory = new TaskHistory {@taskId}
    
        @setView new TailView _.extend {@taskId, @path, @offset},
            collection: @collections.logLines
            model: @models.taskHistory

        app.showView @view

        if @offset?
            $.when( @collections.logLines.fetchOffset(@offset), @refresh() ).then => @view.afterInitialOffsetData()
        else
            $.when( @collections.logLines.fetchInitialData(), @refresh() ).then => @view.afterInitialData()

    refresh: ->
        @models.taskHistory.fetch()


module.exports = TailController
