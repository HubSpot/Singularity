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

        # TaskId to be filled in after active tasks are fetched
        @collections.logLines = new LogLines [], {taskId: "", @path, ajaxError: @models.ajaxError}

        @collections.activeTasks = new RequestTasks [],
            requestId: @requestId
            state:    'active'

        @setView new AggregateTailView _.extend {@requestId, @path, @offset},
            activeTasks: @collections.activeTasks
            logLines: @collections.logLines
            ajaxError: @models.ajaxError

        @fetchCollections()
        app.showView @view

    fetchCollections: ->
      @collections.activeTasks.fetch().done =>
        taskId = @collections.activeTasks.toJSON()[0].id
        # Just using the first task until aggregate endpoint is available
        @collections.logLines.taskId = taskId
        if @offset?
            @collections.logLines.fetchOffset(@offset)
        else
            @collections.logLines.fetchInitialData()


module.exports = AggregateTailController
