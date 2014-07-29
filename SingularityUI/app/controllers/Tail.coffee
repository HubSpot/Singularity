Controller = require './Controller'

LogLines = require '../collections/LogLines'

TailView = require '../views/tail'

class TailController extends Controller

    initialize: ({@taskId, @path}) ->
        @collections.logLines = new LogLines [], {@taskId, @path}
        
        @setView new TailView _.extend {@taskId, @path},
            collection: @collections.logLines

        app.showView @view

        @collections.logLines.fetchInitialData()

module.exports = TailController
