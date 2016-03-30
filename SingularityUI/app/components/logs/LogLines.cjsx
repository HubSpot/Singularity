React = require 'react'
Waypoint = require 'react-waypoint'
LogLine = require './LogLine'
Humanize = require 'humanize-plus'
LogLines = require '../../collections/LogLines'

{ connect } = require 'react-redux'
{ taskGroupTop, taskGroupBottom } = require '../../actions/log'

sum = (numbers) ->
  total = 0
  for n in numbers
    total += n
  total

class LogLines extends React.Component
  @propTypes:
    taskGroupTop: React.PropTypes.func.isRequired
    taskGroupBottom: React.PropTypes.func.isRequired

    taskGroupId: React.PropTypes.number.isRequired
    logLines: React.PropTypes.array.isRequired

    initialDataLoaded: React.PropTypes.bool.isRequired
    reachedStartOfFile: React.PropTypes.bool.isRequired
    reachedEndOfFile: React.PropTypes.bool.isRequired
    bytesRemainingBefore: React.PropTypes.number.isRequired
    bytesRemainingAfter: React.PropTypes.number.isRequired
    activeColor: React.PropTypes.string.isRequired

  componentDidMount: ->
    window.addEventListener 'resize', @handleScroll

  componentWillUnmount: ->
    window.removeEventListener 'resize', @handleScroll

  componentDidUpdate: (prevProps, prevState) ->
    if prevProps.updatedAt isnt @props.updatedAt
      if @props.prependedLineCount > 0
        @refs.tailContents.scrollTop += 20 * @props.prependedLineCount

  renderLoadingPrevious: ->
    if @props.initialDataLoaded
      if @props.reachedStartOfFile
        <div>At beginning of file</div>
      else
        <div>Loading previous... ({Humanize.filesize(@props.bytesRemainingBefore)} remaining)</div>

  renderLogLines: ->
    @props.logLines.map ({data, offset, taskId}) =>
      <LogLine
        content={data}
        key={offset}
        offset={offset}
        taskId={taskId}
        isHighlighted={offset is @props.initialOffset} />

  renderLoadingMore: ->
    if @props.initialDataLoaded
      if @props.reachedEndOfFile
        <div>Tailing...</div>
      else
        <div>Loading more... ({Humanize.filesize(@props.bytesRemainingAfter)} remaining)</div>

  handleScroll: =>
    {scrollTop, scrollHeight, clientHeight} = @refs.tailContents

    if scrollTop < clientHeight
      @props.taskGroupTop(@props.taskGroupId, true)
    else
      @props.taskGroupTop(@props.taskGroupId, false)

    if scrollTop + clientHeight > scrollHeight - clientHeight
      @props.taskGroupBottom(@props.taskGroupId, true)
    else
      @props.taskGroupBottom(@props.taskGroupId, false)

  render: ->
    <div className="contents-container">
      <div className="tail-contents #{@props.activeColor}" tabIndex="1" ref="tailContents" onScroll={@handleScroll}>
        {@renderLoadingPrevious()}
        {@renderLogLines()}
        {@renderLoadingMore()}
      </div>
    </div>

mapStateToProps = (state, ownProps) ->
  taskGroup = state.taskGroups[ownProps.taskGroupId]
  tasks = taskGroup.taskIds.map (taskId) -> state.tasks[taskId]

  logLines: taskGroup.logLines
  updatedAt: taskGroup.updatedAt
  prependedLineCount: taskGroup.prependedLineCount
  activeColor: state.activeColor
  top: taskGroup.top
  bottom: taskGroup.bottom
  initialDataLoaded: _.all(_.pluck(tasks, 'initialDataLoaded'))
  reachedStartOfFile: _.all(tasks.map (task) -> task.minOffset is 0)
  reachedEndOfFile: _.all(tasks.map (task) -> task.maxOffset >= task.filesize)
  bytesRemainingBefore: sum(_.pluck(tasks, 'minOffset'))
  bytesRemainingAfter: sum(tasks.map (task) -> Math.max(task.filesize - task.maxOffset, 0))

mapDispatchToProps = { taskGroupTop, taskGroupBottom }

module.exports = connect(mapStateToProps, mapDispatchToProps)(LogLines)
