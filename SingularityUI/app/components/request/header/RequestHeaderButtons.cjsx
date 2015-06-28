RequestButton = require './RequestButton'


RequestHeaderButtons = React.createClass

  displayName: 'RequestHeaderButtons'

  # propTypes:

  render: ->
    SPACE = ' '
    <div>
      <RequestButton bsStyle='default' id='' action='viewObjectJSON'>
        JSON
      </RequestButton>
      {SPACE}
      <RequestButton bsStyle='primary' id='' action='scale'>
        Scale
      </RequestButton>
      {SPACE}
      <RequestButton bsStyle='primary' id='' action='pause'>
        Pause
      </RequestButton>
      {SPACE}
      <RequestButton bsStyle='primary' id='' action='bounce'>
        Bounce
      </RequestButton>
      {SPACE}
      <RequestButton bsStyle='danger' id='' action='remove'>
        Remove
      </RequestButton>
      {SPACE}
      <RequestButton bsStyle='success' id='' link='appRoot/request/data.id/deploy' action='deploy'>
        Deploy
      </RequestButton>
      {SPACE}
      <RequestButton bsStyle='primary' id='' action='run-request-now'>
        Run now
      </RequestButton>
      {SPACE}
      <RequestButton bsStyle='primary' id='' action='unpause'>
        Remove
      </RequestButton>
    </div>

module.exports = RequestHeaderButtons