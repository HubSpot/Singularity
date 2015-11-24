SimpleSubview = require './simpleSubview'

class taskShellCommandsSubview extends SimpleSubview

    initialize: (@params) ->
      super(@params)
      @selectedCommandIndex = 0
      @selectedCommandDescription = null

    render: =>
      super()
      $("#cmd").prop('selectedIndex', @selectedCommandIndex);
      if @selectedCommandDescription
        $('.cmd-description').text(@selectedCommandDescription)

module.exports = taskShellCommandsSubview
