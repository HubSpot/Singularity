Controller = require './Controller'

LogLines = require '../collections/LogLines'
TaskHistory = require '../models/TaskHistory'
AjaxError = require '../models/AjaxError'
RequestTasks = require '../collections/RequestTasks'

AggregateTailView = require '../views/aggregateTail'

class AggregateTailController extends Controller

    initialize: ({@requestId, @path, @offset}) ->
        @title 'Tail of ' + @path

        @models.ajaxError = new AjaxError
        @collections.activeTasks = new RequestTasks [],
            requestId: @requestId
            state:    'active'

        @setView new AggregateTailView _.extend {@requestId, @path, @offset},
            activeTasks: @collections.activeTasks
            logLines: @collections.logLines
            ajaxError: @models.ajaxError

        @refresh
        app.showView @view

    refresh: ->
      @collections.activeTasks.fetch().done =>
        console.log @collections.activeTasks.toJSON()
        taskId = @collections.activeTasks.toJSON()[0].id
        @collections.logLines = new LogLines [], {taskId, @path, ajaxError: @models.ajaxError}
        @collections.logLines.fetch().done =>
          console.log @collections.logLines.toJSON()


module.exports = AggregateTailController
