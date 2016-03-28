React = require 'react'

{ getInstanceNumberFromTaskId } = require '../../utils'

class TaskGroupHeader extends React.Component
  @propTypes:
    taskIds: React.PropTypes.array.isRequired

  toggleLegend: ->
    # TODO

  renderInstanceInfo: ->
    if @props.taskIds.length > 1
      <span className="instance-link">Viewing Instances {@props.taskIds.map(getInstanceNumberFromTaskId).join(', ')}</span>
    else
      <a href={"#{config.appRoot}/task/#{@props.taskIds[0]}"}>Instance {getInstanceNumberFromTaskId(@props.taskIds[0])}</a>

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