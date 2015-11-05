Controller = require './Controller'

LogLines = require '../collections/LogLines'
TaskHistory = require '../models/TaskHistory'
AjaxError = require '../models/AjaxError'
RequestTasks = require '../collections/RequestTasks'

AggregateTailView = require '../views/aggregateTail'

class AggregateTailController extends Controller

    initialize: ({@requestId, @path, @offset}) ->
        @title 'Tail of ' + @path

        @models.ajaxError = []
        @collections.logLines = []

        @collections.activeTasks = new RequestTasks [],
            requestId: @requestId
            state:    'active'

        @view = new AggregateTailView _.extend {@requestId, @path, @offset},
            activeTasks: @collections.activeTasks
            logLines: @collections.logLines
            ajaxError: @models.ajaxError

        @setView @view

        @fetchCollections()
        app.showView @view

    fetchCollections: ->
      @collections.activeTasks.fetch().done =>
        for taskId in @collections.activeTasks.pluck('id')
          @models.ajaxError[taskId] = new AjaxError
          @collections.logLines[taskId] = new LogLines [], {taskId, @path, ajaxError: @models.ajaxError[taskId]}
          if @offset?
              @collections.logLines[taskId].fetchOffset(@offset)
          else
              @collections.logLines[taskId].fetchInitialData()

        @view.render()


module.exports = AggregateTailController
