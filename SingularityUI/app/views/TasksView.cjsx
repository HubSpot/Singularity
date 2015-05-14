
# Request = require '../models/Request'
# Slaves = require '../collections/Slaves'


TasksMain = require '../components/tasks/TasksMainCmpt'
View = require './ReactBaseView'

class TasksView extends View

  initialize: =>
    @refresh()

  refresh: =>
    @collections.tasks.fetch().done =>
      @renderReact()
    @collections.slaves.fetch().done =>
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
    changeFilterState: @changeFilterState


  changeFilterState: (filter) =>
    @filterState = filter
    @renderReact()



module.exports = TasksView