
Utils = require '../../utils'

StatusIndicator = React.createClass

  getClassName: ->
    if @props.status in Utils.TERMINAL_TASK_STATES
      'bg-danger'
    else
      'bg-info running'

  render: ->
    <div className="status">
      <div className="indicator #{@getClassName()}"></div>
      {@props.status?.toLowerCase().replace('_', ' ')}
    </div>

module.exports = StatusIndicator
