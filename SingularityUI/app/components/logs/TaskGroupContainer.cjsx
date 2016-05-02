React = require 'react'
TaskGroupHeader = require './TaskGroupHeader'
LogLines = require './LogLines'
LoadingSpinner = require './LoadingSpinner'
FileNotFound = require './FileNotFound'
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
    if @props.logDataLoaded
      return <LogLines taskGroupId={@props.taskGroupId} />
    else if @props.initialDataLoaded and not @props.fileExists
      return <div className="tail-contents"><FileNotFound fileName={@props.path} /></div>
    else
      return <LoadingSpinner centered={true}>Loading logs...</LoadingSpinner>

  render: ->
    className = "col-md-#{ @getContainerWidth() } tail-column"
    <div className={className}>
      <TaskGroupHeader taskGroupId={@props.taskGroupId} />
      {@renderLogLines()}
    </div>


mapStateToProps = (state, ownProps) ->
  unless ownProps.taskGroupId of state.taskGroups
    return {
      initialDataLoaded: false
      fileExists: false
      logDataLoaded: false
      terminated: false
    }
  taskGroup = state.taskGroups[ownProps.taskGroupId]
  tasks = taskGroup.taskIds.map (taskId) -> state.tasks[taskId]

  initialDataLoaded: _.all(_.pluck(tasks, 'initialDataLoaded'))
  logDataLoaded: _.all(_.pluck(tasks, 'logDataLoaded'))
  fileExists: _.any(_.pluck(tasks, 'exists'))
  terminated: _.all(_.pluck(tasks, 'terminated'))
  path: state.path

module.exports = connect(mapStateToProps)(TaskGroupContainer)