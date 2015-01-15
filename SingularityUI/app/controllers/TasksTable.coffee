Controller = require './Controller'

Tasks = require '../collections/Tasks'
Slaves = require '../collections/Slaves'

TasksTableView = require '../views/tasks'

class TasksTableController extends Controller

    initialize: ({@state, @searchFilter}) ->
        # We want the view to handle the page loader for this one
        console.dir(@state)
        if @state is 'decommissioning'
            @collections.tasks = new Tasks [], state: 'active'
        else
            @collections.tasks = new Tasks [], {@state}
        @collections.slaves = new Slaves []

        @setView new TasksTableView _.extend {@state, @searchFilter},
            collection: @collections.tasks
            attributes:
                slaves: @collections.slaves

        @collections.slaves.fetch()
        @collections.tasks.fetch()
        app.showView @view

    refresh: ->
        # Don't refresh if user is scrolled down, viewing the table (arbitrary value)
        return if $(window).scrollTop() > 200
        # Don't refresh if the table is sorted
        return if @view.isSorted

        @collections.slaves.fetch()
        @collections.tasks.fetch reset: true

module.exports = TasksTableController
