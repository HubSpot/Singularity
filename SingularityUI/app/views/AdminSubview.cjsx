AdminMainCmpt = require '../components/admin/AdminMainCmpt'

##
## Used by Slave and Rack views
##
View = require './ReactBaseView'

class AdminSubview extends View

  initialize: (@options) ->
    @refresh()

  renderReact: ->
    React.render(
        <AdminMainCmpt
          label={@options.label}
          data={@getRenderData()}
          actions={@actions}
        />, 
        app.pageEl
      )    

  refresh: =>
    @collection.fetch({reset:true}).done =>
      @renderReact()

  ##
  ## Render Dasta
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


module.exports = AdminSubview