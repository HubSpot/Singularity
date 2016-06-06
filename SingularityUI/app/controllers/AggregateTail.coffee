Controller = require './Controller'

LogLines = require '../collections/LogLines'
TaskHistory = require '../models/TaskHistory'
AjaxError = require '../models/AjaxError'
RequestTasks = require '../collections/RequestTasks'

AggregateTailView = require('../views/aggregateTail').default

Utils = require '../utils'

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
        taskIds = @collections.activeTasks.pluck('id')
        params = Utils.getQueryParams()
        if params.taskIds
          taskIds = _.union taskIds, params.taskIds.split(',')

        for taskId in taskIds
          @models.ajaxError[taskId] = new AjaxError
          path = @path.replace('$TASK_ID', taskId)
          @collections.logLines[taskId] = new LogLines [], {taskId, path: path, ajaxError: @models.ajaxError[taskId]}

        @view.render()


module.exports = AggregateTailController
