
# Request = require '../models/Request'
# Slaves = require '../collections/Slaves'


TasksMain = require '../components/tasks/TasksMain'
View = require './ReactBaseView'

class TasksView extends View

  initialize: =>
    console.log 'init'
    @refresh()

  refresh: =>
    ## TO DO: show a loader
    # $.when( @collections.tasks.fetch(), @collections.slaves.fetch() ).done =>
    @collections.tasks.fetch().done =>
      console.log 'refresh done: ', @collections.tasks.toJSON().length
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