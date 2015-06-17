BaseAdminView = require './BaseAdminView'
SlavesMainComponent = require '../components/admin/SlavesMain'


class SlavesView extends BaseAdminView

  initialize: (@options) ->
    @refresh()

  renderReact: ->
    React.render(
        <SlavesMainComponent
          data={@getRenderData()}
          actions={@actions}
        />, 
        app.pageEl
      )


module.exports = SlavesView