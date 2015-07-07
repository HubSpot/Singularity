
TasksMain = require '../components/tasks/TasksMain'
View = require './ReactBaseView'

class TasksView extends View

  synced: false

  renderAtOnce: 50,
  tasksRendered: 0,
  tasksToRender: [],

  initialize: =>
    @activeTable = @options.state
    @renderMore = @renderMore.bind(@)
    @renderReact()
    @refresh()

  refresh: =>
    # Don't refresh if user is scrolled down, viewing the table (arbitrary value)
    return if $(window).scrollTop() > 200

    # To Do
    # Don't refresh if the table is sorted
    # return if @view.isSorted
    $.when( @collections.tasks.fetch({reset: true}), @collections.slaves.fetch() ).done =>
      @decommissioning_hosts = @collections.slaves.decommissioning_hosts()
      @synced = true
      @renderReact()

  renderReact: ->
    @tasksToRender = _.pluck @collections.tasks.slice(0, @tasksRendered + @renderAtOnce), 'attributes'

    React.render(
      <TasksMain
        data={@getRenderData()}
        actions={@actions}
      />, app.pageEl
    )

  changeTable: (filterState) =>
    ## To do: handle decommissioning table
    @collections.tasks.setState filterState
    @filterState = filterState
    @refresh()

  renderMore: ->
    @tasksRendered = @tasksRendered + @renderAtOnce
    if (@tasksToRender.length + @renderAtOnce) >= @collections.tasks.length
      return false
    @renderReact()

  getRenderData: ->
    tasks: @tasksToRender
    decommissioning_hosts: @decommissioning_hosts
    searchFilter: @options.searchFilter
    initialFilterState: @options.state
    activeTable: @activeTable
    filterState: @filterState || ''
    synced: @synced

  actions: =>
    changeTable: @changeTable
    renderMore: @renderMore


module.exports = TasksView