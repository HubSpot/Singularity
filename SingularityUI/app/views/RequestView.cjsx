RequestMain = require '../components/request/RequestMain'
View = require './ReactBaseView'

class RequestView extends View

  synced: false

  initialize: =>
    @renderReact()
    @refresh()

  refresh: =>

  renderReact: ->
    React.render(
      <RequestMain
        data={@getRenderData()}
        actions={@actions}
      />, app.pageEl
    )

  getRenderData: ->
    synced: @synced

  actions: =>
    

module.exports = RequestView