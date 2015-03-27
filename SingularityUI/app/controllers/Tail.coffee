Controller = require './Controller'

LogLines = require '../collections/LogLines'
TaskHistory = require '../models/TaskHistory'

TailView = require '../views/tail'

class TailController extends Controller

    initialize: ({@taskId, @path}) ->
        @collections.logLines = new LogLines [], {@taskId, @path}
        @models.taskHistory = new TaskHistory {@taskId}
    
        @setView new TailView _.extend {@taskId, @path},
            collection: @collections.logLines
            model: @models.taskHistory

        app.showView @view
        @refresh()

    refresh: ->
        @collections.logLines.fetchInitialData()
        @models.taskHistory.fetch()


module.exports = TailController
