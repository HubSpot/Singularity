React = require 'react'
TaskGroupHeader = require './TaskGroupHeader'
LogLines = require './LogLines'
classNames = require 'classnames'

{ connect } = require 'react-redux'

class TaskGroupContainer extends React.Component
  @propTypes:
    taskGroupId: React.PropTypes.number.isRequired
    path: React.PropTypes.string.isRequired
    taskGroupCount: React.PropTypes.number.isRequired

  render: ->
    className = "col-md-#{ 12 / @props.taskGroupCount } tail-column"
    <div className={className}>
      <TaskGroupHeader taskIds={@props.taskIds} />
      <LogLines taskGroupId={@props.taskGroupId} />
    </div>


mapStateToProps = (state, ownProps) ->
  taskGroup = state.taskGroups[ownProps.taskGroupId]
  tasks = taskGroup.taskIds.map (taskId) -> state.tasks[taskId]

  taskIds: taskGroup.taskIds
  logLines: taskGroup.logLines
  search: taskGroup.search
  ready: taskGroup.ready
  taskGroupCount: state.taskGroups.length
  permalinkEnabled: tasks.length is 1
  activeColor: state.activeColor
  path: state.path

module.exports = connect(mapStateToProps)(TaskGroupContainer)