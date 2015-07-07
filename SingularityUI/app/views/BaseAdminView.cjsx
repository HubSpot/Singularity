##
## Used by Slave and Rack Views
##

View = require './ReactBaseView'

class BaseAdminView extends View

  refresh: =>
    @collection.fetch({reset:true}).done =>
      @renderReact()

  ##
  ## Render Data
  ##
  getRenderData: ->
    active: @collection.filterByState(['ACTIVE'])
    decomm: @collection.filterByState(['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION'])
    inactive: @collection.filterByState(['DEAD', 'MISSING_ON_STARTUP'])

  ##
  ## Actions
  ##
  actions: =>
    changeItemState: @changeItemState

  changeItemState: (item) =>
    model = new @options.model
      id:    item.id
      host:  item.host
      state: item.state

    if item.action is 'decommission'
      model.promptDecommission => 
        @renderReact()

    if item.action is 'reactivate'
      model.promptReactivate => 
        @renderReact()

    if item.action is 'remove'
      model.promptRemove =>
        @renderReact()


module.exports = BaseAdminView