React = require 'react'
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
      if @props.tailing
        @refs.tailContents.scrollTop = @refs.tailContents.scrollHeight
      else if @props.prependedLineCount > 0 or @props.linesRemovedFromTop > 0
        @refs.tailContents.scrollTop += 20 * (@props.prependedLineCount - @props.linesRemovedFromTop)
      else
        @handleScroll()

  renderLoadingPrevious: ->
    if @props.initialDataLoaded
      if not @props.reachedStartOfFile
        <div>Loading previous... ({Humanize.filesize(@props.bytesRemainingBefore)} remaining)</div>

  renderLogLines: ->
    @props.logLines.map ({data, offset, taskId, timestamp}) =>
      <LogLine
        content={data}
        key={taskId + '_' + offset}
        offset={offset}
        taskId={taskId}
        timestamp={timestamp}
        isHighlighted={offset is @props.initialOffset}
        color={@props.colorMap[taskId]} />

  renderLoadingMore: ->
    if @props.terminated
      return null
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
      @props.taskGroupBottom(@props.taskGroupId, true, (scrollTop + clientHeight > scrollHeight - 20))
    else
      @props.taskGroupBottom(@props.taskGroupId, false)

  render: ->
    <div className="contents-container">
      <div className="tail-contents #{@props.activeColor}" ref="tailContents" onScroll={@handleScroll}>
        {@renderLoadingPrevious()}
        {@renderLogLines()}
        {@renderLoadingMore()}
      </div>
    </div>

mapStateToProps = (state, ownProps) ->
  taskGroup = state.taskGroups[ownProps.taskGroupId]
  tasks = taskGroup.taskIds.map (taskId) -> state.tasks[taskId]

  colorMap = {}
  if taskGroup.taskIds.length > 1
    i = 0
    for taskId in taskGroup.taskIds
      colorMap[taskId] = "hsla(#{(360 / taskGroup.taskIds.length) * i}, 100%, 50%, 0.1)"
      i++

  logLines: taskGroup.logLines
  updatedAt: taskGroup.updatedAt
  tailing: taskGroup.tailing
  prependedLineCount: taskGroup.prependedLineCount
  linesRemovedFromTop: taskGroup.linesRemovedFromTop
  activeColor: state.activeColor
  top: taskGroup.top
  bottom: taskGroup.bottom
  initialDataLoaded: _.all(_.pluck(tasks, 'initialDataLoaded'))
  terminated: _.all(_.pluck(tasks, 'terminated'))
  reachedStartOfFile: _.all(tasks.map ({minOffset}) -> minOffset is 0)
  reachedEndOfFile: _.all(tasks.map ({maxOffset, filesize}) -> maxOffset >= filesize)
  bytesRemainingBefore: sum(_.pluck(tasks, 'minOffset'))
  bytesRemainingAfter: sum(tasks.map ({filesize, maxOffset}) -> Math.max(filesize - maxOffset, 0))
  colorMap: colorMap

mapDispatchToProps = { taskGroupTop, taskGroupBottom }

module.exports = connect(mapStateToProps, mapDispatchToProps)(LogLines)
