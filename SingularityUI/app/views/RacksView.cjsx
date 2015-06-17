BaseAdminView = require './BaseAdminView'
RacksMainComponent = require '../components/admin/RacksMain'


class RacksView extends BaseAdminView

  initialize: (@options) ->
    @refresh()

  renderReact: ->
    React.render(
        <RacksMainComponent
          data={@getRenderData()}
          actions={@actions}
        />, 
        app.pageEl
      )


module.exports = RacksView