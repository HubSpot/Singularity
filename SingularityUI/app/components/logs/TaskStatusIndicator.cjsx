React = require 'react'
Utils = require '../../utils'

class TaskStatusIndicator extends React.Component
  @propTypes:
    status: React.PropTypes.string

  getClassName: ->
    if @props.status in Utils.TERMINAL_TASK_STATES
      'bg-danger'
    else
      'bg-info running'

  render: ->
    if @props.status
      <div className="status">
        <div className="indicator #{@getClassName()}"></div>
        {@props.status.toLowerCase().replace('_', ' ')}
      </div>
    else
      <div />

module.exports = TaskStatusIndicator
