
# Request = require '../models/Request'
# Slaves = require '../collections/Slaves'


TasksMain = require '../components/tasks/TasksMain'
View = require './ReactBaseView'

class TasksView extends View

  initialize: =>
    @renderReact()
    @refresh()

  refresh: =>
    # $.when( @collections.tasks.fetch(), @collections.slaves.fetch() ).done =>
    @collections.tasks.fetch().done =>
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
    searchFilter: @options.searchFilter
    initialFilterState: @options.state
    filterState: @filterState || ''

  ##
  ## Actions
  ##
  actions: =>


module.exports = TasksView