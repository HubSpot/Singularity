Controller = require './Controller'

TaskSearchView = require '../views/taskSearch'

Utils = require '../utils'

class TaskSearchController extends Controller

    handleSubmit: (params) =>
        @params = params
        @formSubmitted = true
        @setUpView()
        @view.render()

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

        #@fetchCollections()
        @view.render()
        app.showView @view



module.exports = TaskSearchController