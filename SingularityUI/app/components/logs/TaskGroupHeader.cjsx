React = require 'react'

class TaskGroupHeader extends React.Component
  @propTypes:
    taskIds: React.PropTypes.array.isRequired

  toggleLegend: ->
    # TODO

  getInstanceNumberFromTaskId: (taskId) ->
    splits = taskId.split('-')
    splits[splits.length - 3]

  renderInstanceInfo: ->
    if @props.taskIds.length > 1
      <span className="instance-link">Viewing Instances {@props.taskIds.map(@getInstanceNumberFromTaskId).join(', ')}</span>
    else
      <span>Instance {@getInstanceNumberFromTaskId(@props.taskIds[0])}</span>

  renderTaskLegend: ->
    if @props.taskIds.length > 1
      <span className="right-buttons">
        <a className="action-link" onClick={@toggleLegend}><span className="glyphicon glyphicon-menu-hamburger"></span></a>
      </span>

  render: ->
    <div className="individual-header">
      {@renderInstanceInfo()}
      {@renderTaskLegend()}
    </div>
    # TODO: renderLegend()

module.exports = TaskGroupHeader