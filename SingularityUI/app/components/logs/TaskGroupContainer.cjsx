React = require 'react'
TaskGroupHeader = require './TaskGroupHeader'
LogLines = require './LogLines'
LoadingSpinner = require './LoadingSpinner'
classNames = require 'classnames'

{ connect } = require 'react-redux'

class TaskGroupContainer extends React.Component
  @propTypes:
    taskGroupId: React.PropTypes.number.isRequired
    taskGroupContainerCount: React.PropTypes.number.isRequired

    initialDataLoaded: React.PropTypes.bool.isRequired
    fileExists: React.PropTypes.bool.isRequired
    terminated: React.PropTypes.bool.isRequired

  getContainerWidth: ->
    return (12 / @props.taskGroupContainerCount)

  renderLogLines: ->
    if @props.initialDataLoaded
      if @props.fileExists
        return <LogLines taskGroupId={@props.taskGroupId} />
      else
        return <h3>File does not exist</h3>
    else
      return <LoadingSpinner centered={true}>Loading logs...</LoadingSpinner>

  render: ->
    className = "col-md-#{ @getContainerWidth() } tail-column"
    <div className={className}>
      <TaskGroupHeader taskGroupId={@props.taskGroupId} />
      {@renderLogLines()}
    </div>


mapStateToProps = (state, ownProps) ->
  taskGroup = state.taskGroups[ownProps.taskGroupId]
  tasks = taskGroup.taskIds.map (taskId) -> state.tasks[taskId]

  initialDataLoaded: _.all(_.pluck(tasks, 'logDataLoaded'))
  fileExists: _.any(_.pluck(tasks, 'exists'))
  terminated: _.all(_.pluck(tasks, 'terminated'))

module.exports = connect(mapStateToProps)(TaskGroupContainer)