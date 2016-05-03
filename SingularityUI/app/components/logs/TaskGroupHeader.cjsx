React = require 'react'
TaskStatusIndicator = require './TaskStatusIndicator'
OverlayTrigger = require 'react-bootstrap/lib/OverlayTrigger'
ToolTip = require 'react-bootstrap/lib/Tooltip'

{ getInstanceNumberFromTaskId, getDeployIdFromTaskId, getHostFromTaskId } = require '../../utils'

{ connect } = require 'react-redux'

{ removeTaskGroup, expandTaskGroup, scrollToTop, scrollToBottom } = require '../../actions/log'

class TaskGroupHeader extends React.Component
  @propTypes:
    taskGroupId: React.PropTypes.number.isRequired
    tasks: React.PropTypes.array.isRequired

  toggleLegend: ->
    # TODO

  getToolTip: (task) ->
    <ToolTip id={task.taskId}>Deploy ID: {getDeployIdFromTaskId task.taskId}<br />Host: {getHostFromTaskId task.taskId}</ToolTip>

  renderInstanceInfo: ->
    if @props.tasks.length > 1
      <span className="instance-link">Viewing Instances {@props.tasks.map(({taskId}) -> getInstanceNumberFromTaskId(taskId)).join(', ')}</span>
    else if @props.tasks.length > 0
      <span>
        <div className="width-constrained">
          <OverlayTrigger placement='bottom' overlay={@getToolTip @props.tasks[0]}>
            <a className="instance-link" href={"#{config.appRoot}/task/#{@props.tasks[0].taskId}"}>Instance {getInstanceNumberFromTaskId(@props.tasks[0].taskId)}</a>
          </OverlayTrigger>
        </div>
        <TaskStatusIndicator status={@props.tasks[0].lastTaskStatus} />
      </span>
    else
      <div className="width-constrained" />

  renderTaskLegend: ->
    if @props.tasks.length > 1
      <span className="right-buttons">
        <a className="action-link" onClick={@toggleLegend}><span className="glyphicon glyphicon-menu-hamburger"></span></a>
      </span>

  renderClose: ->
    if @props.taskGroupsCount > 1
      <a className="action-link" onClick={() => @props.removeTaskGroup(@props.taskGroupId)} title="Close Task"><span className="glyphicon glyphicon-remove"></span></a>

  renderExpand: ->
    if @props.taskGroupsCount > 1
      <a className="action-link" onClick={() => @props.expandTaskGroup(@props.taskGroupId)} title="Show only this Task"><span className="glyphicon glyphicon-resize-full"></span></a>

  render: ->
    <div className="individual-header">
      {@renderClose()}
      {@renderExpand()}
      {@renderInstanceInfo()}
      {@renderTaskLegend()}
      <span className="right-buttons">
        <a className="action-link" onClick={() => @props.scrollToBottom(@props.taskGroupId) } title="Scroll to Bottom"><span className="glyphicon glyphicon-chevron-down"></span></a>
        <a className="action-link" onClick={() => @props.scrollToTop(@props.taskGroupId) } title="Scroll to Top"><span className="glyphicon glyphicon-chevron-up"></span></a>
      </span>
    </div>

mapStateToProps = (state, ownProps) ->
  unless ownProps.taskGroupId of state.taskGroups
    return {
      taskGroupsCount: state.taskGroups.length
      tasks: []
    }
  taskGroupsCount: state.taskGroups.length
  tasks: state.taskGroups[ownProps.taskGroupId].taskIds.map (taskId) -> state.tasks[taskId]

mapDispatchToProps = { scrollToTop, scrollToBottom, removeTaskGroup, expandTaskGroup }

module.exports = connect(mapStateToProps, mapDispatchToProps)(TaskGroupHeader)
