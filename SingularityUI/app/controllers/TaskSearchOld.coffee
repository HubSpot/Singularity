Controller = require './Controller'

TaskSearchView = require('../views/taskSearch').default

Utils = require '../utils'

class TaskSearchController extends Controller


    initialize: ({@requestId}) ->
        @formSubmitted = false
        @title 'Task Search'
        @params = {}
        if @requestId
            @global = false
        else
            @global = true
        @view = new TaskSearchView
            requestId : @requestId
            global : @global
        @setView @view

        @view.render()
        app.showView @view



module.exports = TaskSearchController
