Controller = require './Controller'

TaskSearchView = require '../views/taskSearch'
TaskSearchResults = require '../collections/TaskSearchResults'

Utils = require '../utils'

class TaskSearchController extends Controller


    initialize: ({@requestId}) ->
        @formSubmitted = false
        @title 'Task Search'
        @params = {}
        if @requestId
        	@requestLocked = true
        else
        	@requestLocked = false
        @view = new TaskSearchView
            requestId : @requestId
            requestLocked : @requestLocked
        @setView @view

        @view.render()
        app.showView @view



module.exports = TaskSearchController