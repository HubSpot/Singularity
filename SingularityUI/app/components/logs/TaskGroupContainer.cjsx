React = require 'react'
TaskGroupHeader = require './TaskGroupHeader'
LogLines = require './LogLines'
classNames = require 'classnames'

{ connect } = require 'react-redux'
{ taskGroupTop, taskGroupBottom, clickPermalink } = require '../../actions/log'

sum = (numbers) ->
  total = 0
  for n in numbers
    total += n
  total

class TaskGroupContainer extends React.Component
  @propTypes:
    taskGroupId: React.PropTypes.number.isRequired
    path: React.PropTypes.string.isRequired

  render: ->
    className = "col-md-#{ 12 / @props.taskGroupCount } tail-column"
    <div className={className}>
      <TaskGroupHeader taskIds={@props.taskIds} />
      <LogLines {...@props} />
    </div>


mapStateToProps = (state, ownProps) ->
  taskGroup = state.taskGroups[ownProps.taskGroupId]
  tasks = taskGroup.taskIds.map (taskId) -> ownProps.tasks[taskId]

  taskIds: taskGroup.taskIds
  logLines: taskGroup.logLines
  search: taskGroup.search
  ready: taskGroup.ready
  taskGroupCount: state.taskGroups.length
  initialDataLoaded: _.all(_.pluck(tasks, 'initialDataLoaded'))
  reachedStartOfFile: _.all(tasks.map (task) -> task.minOffset is 0)
  reachedEndOfFile: _.all(tasks.map (task) -> task.maxOffset >= task.filesize)
  bytesRemainingBefore: sum(_.pluck(tasks, 'minOffset'))
  bytesRemainingAfter: sum(tasks.map (task) -> Math.max(task.filesize - task.maxOffset, 0))
  permalinkEnabled: tasks.length is 1
  activeColor: state.activeColor

mapDispatchToProps = (dispatch) ->
  onEnterTop: (taskGroupId) -> dispatch(taskGroupTop(taskGroupId, true))
  onEnterBottom: (taskGroupId) -> dispatch(taskGroupBottom(taskGroupId, true))
  onLeaveTop: (taskGroupId) -> dispatch(taskGroupTop(taskGroupId, false))
  onLeaveBottom: (taskGroupId) -> dispatch(taskGroupBottom(taskGroupId, false))
  onPermalinkClick: (offset) -> dispatch(clickPermalink(offset))

module.exports = connect(mapStateToProps, mapDispatchToProps)(TaskGroupContainer)