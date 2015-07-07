Controller = require './ReactController'
TasksView = require '../views/TasksView'

Tasks = require '../collections/Tasks'
Slaves = require '../collections/Slaves'

class TasksController extends Controller

  initialize: ({@state, @searchFilter}) ->
    app.showPageLoader()

    if @state is 'decommissioning'
      @collections.tasks = new Tasks [], state: 'active'
    else
      @collections.tasks = new Tasks [], {@state}
    @collections.slaves = new Slaves []

    new TasksView
      collections:
        tasks: @collections.tasks
        slaves: @collections.slaves
      state: @state
      searchFilter: @searchFilter

module.exports = TasksController



#     refresh: ->
#         # Don't refresh if user is scrolled down, viewing the table (arbitrary value)
#         return if $(window).scrollTop() > 200
#         # Don't refresh if the table is sorted
#         return if @view.isSorted

#         @collections.slaves.fetch()
#         @collections.tasks.fetch reset: true

# module.exports = TasksTableController
