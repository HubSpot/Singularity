Controller = require './Controller'

TaskSearchView = require '../views/taskSearch'

Utils = require '../utils'

class TaskSearchController extends Controller

    initialize: ({@requestLocked, @requestId}) ->
        @title 'Task Search'
        @view = new TaskSearchView @requestId
        @setView @view

        #@fetchCollections()
        @view.render()
        app.showView @view



module.exports = TaskSearchController