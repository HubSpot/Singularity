
TasksMain = require '../components/tasks/TasksMain'
View = require './ReactBaseView'

class TasksView extends View

  synced: false

  initialize: =>
    @activeTable = @options.state
    @renderReact()
    @refresh()

  refresh: =>
    $.when( @collections.tasks.fetch({reset: true}), @collections.slaves.fetch() ).done =>
      @decommissioning_hosts = @collections.slaves.decommissioning_hosts()
      @synced = true
      @renderReact()

  renderReact: ->
    React.render(
      <TasksMain
        data={@getRenderData()}
        actions={@actions}
      />, app.pageEl
    )

  ##
  ## Render Data
  ##
  getRenderData: ->
    tasks: @collections.tasks.toJSON()
    decommissioning_hosts: @decommissioning_hosts
    searchFilter: @options.searchFilter
    initialFilterState: @options.state
    activeTable: @activeTable
    filterState: @filterState || ''
    synced: @synced

  ##
  ## Actions
  ##
  actions: =>
    changeTable: @changeTable

  changeTable: (filterState) =>
    ## To do: handle decommissioning table
    @collections.tasks.setState filterState
    @filterState = filterState
    @refresh()

module.exports = TasksView