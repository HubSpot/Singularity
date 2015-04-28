Controller = require './Controller'

LogLines = require '../collections/LogLines'
TaskHistory = require '../models/TaskHistory'
AjaxError = require '../models/AjaxError'

TailView = require '../views/tail'

class TailController extends Controller

    initialize: ({@taskId, @path, @offset}) ->
        @models.ajaxError = new AjaxError
        @collections.logLines = new LogLines [], {@taskId, @path, ajaxError: @models.ajaxError}
        @models.taskHistory = new TaskHistory {@taskId}
    
        @setView new TailView _.extend {@taskId, @path, @offset},
            collection: @collections.logLines
            model: @models.taskHistory
            ajaxError: @models.ajaxError

        app.showView @view
        if @offset?
            @collections.logLines.fetchOffset @offset
        else
            @collections.logLines.fetchInitialData()

        @refresh()

    refresh: ->
        @models.taskHistory.fetch()


module.exports = TailController
