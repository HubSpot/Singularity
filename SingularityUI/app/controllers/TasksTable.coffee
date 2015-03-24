Controller = require './Controller'

TaskPending = require '../models/TaskPending'

Tasks = require '../collections/Tasks'
TasksPending = require '../collections/TasksPending'
Slaves = require '../collections/Slaves'

TasksTableView = require '../views/tasks'

class TasksTableController extends Controller

    initialize: ({@state, @searchFilter}) ->
        # We want the view to handle the page loader for this one
        if @state is 'decommissioning'
            @collections.tasks = new Tasks [], state: 'active'
        else if @state is 'scheduled'
            @collections.tasks = new Tasks [], {state: 'scheduled', addPropertyString: 'pendingTask'}
            # @collections.tasks.setPropertyString('pendingTask')
            
        else
            @collections.tasks = new Tasks [], {@state}
        @collections.slaves = new Slaves []

        @setView new TasksTableView _.extend {@state, @searchFilter},
            collection: @collections.tasks
            pendingTasks: @collections.tasksPending
            attributes:
                slaves: @collections.slaves

        # Fetch a pending task's full details
        @view.on 'getPendingTask', (task) => @getPendingTask(task)
        
        @collections.slaves.fetch()
        @collections.tasks.fetch()
        app.showView @view

    getPendingTask: (task) ->
        ## NEED A LOADER
        app.appendPageLoader()
        @collections.tasksPending = new TasksPending [], {requestID: task.requestId}
        @collections.tasksPending.fetch().done =>
            utils.viewJSON @collections.tasksPending.getTaskByRuntime task.nextRunAt
            app.hidePageLoader()


    refresh: ->
        # Don't refresh if user is scrolled down, viewing the table (arbitrary value)
        return if $(window).scrollTop() > 200
        # Don't refresh if the table is sorted
        return if @view.isSorted

        @collections.slaves.fetch()
        @collections.tasks.fetch reset: true

module.exports = TasksTableController
