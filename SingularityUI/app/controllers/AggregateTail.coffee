Controller = require './Controller'

LogLines = require '../collections/LogLines'
TaskHistory = require '../models/TaskHistory'
AjaxError = require '../models/AjaxError'

AggregateTailView = require '../views/aggregateTail'

class AggregateTailController extends Controller

    initialize: ({@requestId, @path, @offset}) ->
        @title 'Tail of ' + @path

        @models.ajaxError = new AjaxError

        @setView new AggregateTailView _.extend {@taskId, @path, @offset},
            collection: null
            model: null
            ajaxError: @models.ajaxError

        app.showView @view

    refresh: ->


module.exports = AggregateTailController
