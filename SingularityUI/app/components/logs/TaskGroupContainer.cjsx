React = require 'react'
TaskGroupHeader = require './TaskGroupHeader'
LogLines = require './LogLines'
classNames = require 'classnames'

{ connect } = require 'react-redux'

class TaskGroupContainer extends React.Component
  @propTypes:
    taskGroupId: React.PropTypes.number.isRequired
    taskGroupCount: React.PropTypes.number.isRequired

  render: ->
    className = "col-md-#{ 12 / @props.taskGroupCount } tail-column"
    <div className={className}>
      <TaskGroupHeader taskGroupId={@props.taskGroupId} />
      <LogLines taskGroupId={@props.taskGroupId} />
    </div>


mapStateToProps = (state) ->
  taskGroupCount: state.taskGroups.length

module.exports = connect(mapStateToProps)(TaskGroupContainer)