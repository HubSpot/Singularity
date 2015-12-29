Controller = require './Controller'

LogLines = require '../collections/LogLines'
TaskHistory = require '../models/TaskHistory'
AjaxError = require '../models/AjaxError'
RequestTasks = require '../collections/RequestTasks'

AggregateTailView = require '../views/aggregateTail'

Utils = require '../utils'

class TailController extends Controller

    initialize: ({@taskId, @path, @offset}) ->
        @title 'Tail of ' + @path

        task = new TaskHistory {taskId: @taskId}
        task.fetch().done =>
            @requestId = task.get('task').taskId.requestId

            @models.ajaxError = []
            @collections.logLines = []

            @collections.activeTasks = new RequestTasks [],
                requestId: @requestId
                state:    'active'

            @view = new AggregateTailView _.extend {@requestId, @path, @offset},
                activeTasks: @collections.activeTasks
                logLines: @collections.logLines
                ajaxError: @models.ajaxError
                singleMode: true
                singleModeTaskId: @taskId

            @setView @view

            @fetchCollections()
            app.showView @view

    fetchCollections: ->
      @collections.activeTasks.fetch().done =>
        @models.ajaxError[@taskId] = new AjaxError
        path = @path.replace('$TASK_ID', @taskId)
        @collections.logLines[@taskId] = new LogLines [], {@taskId, path: path, ajaxError: @models.ajaxError[@taskId]}

        @view.render()


module.exports = TailController
