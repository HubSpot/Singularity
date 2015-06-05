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
            $.when( @models.taskHistory.fetch(), @collections.logLines.fetchOffset(@offset) ).then =>
                @view.afterInitialData()
        else
            $.when( @models.taskHistory.fetch(), @collections.logLines.fetchInitialData() ).then =>
                @view.afterInitialData()
        
        @refresh()

    refresh: ->
        @models.taskHistory.fetch()


module.exports = TailController
