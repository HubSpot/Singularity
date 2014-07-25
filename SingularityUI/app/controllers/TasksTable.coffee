Controller = require './Controller'

Tasks = require '../collections/Tasks'

TasksTableView = require '../views/tasks'

class TasksTableController extends Controller

    initialize: ({@state, @searchFilter}) ->
        # We want the view to handle the page loader for this one
        @collections.tasks = new Tasks [], {@state}

        @view = new TasksTableView _.extend {@state, @searchFilter},
            collection:   @collections.tasks
            controller:   @

        @collections.tasks.fetch()
        
        app.showView @view

    refresh: ->
        # Don't refresh if user is scrolled down, viewing the table (arbitrary value)
        return if $(window).scrollTop() > 200
        # Don't refresh if the table is sorted
        return if @view.isSorted

        @collections.tasks.fetch()

module.exports = TasksTableController
