
# Request = require '../models/Request'
# Slaves = require '../collections/Slaves'


TasksMain = require '../components/tasks/TasksMain'
View = require './ReactBaseView'

class TasksView extends View

  synced: false

  initialize: =>
    @renderReact()
    @refresh()

  refresh: =>
    $.when( @collections.tasks.fetch(), @collections.slaves.fetch() ).done =>
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
    slaves: @collections.slaves.toJSON()
    searchFilter: @options.searchFilter
    initialFilterState: @options.state
    filterState: @filterState || ''
    synced: @synced

  ##
  ## Actions
  ##
  actions: =>


module.exports = TasksView