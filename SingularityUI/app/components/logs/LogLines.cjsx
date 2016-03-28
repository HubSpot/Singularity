React = require 'react'
Waypoint = require 'react-waypoint'
LogLine = require './LogLine'
Humanize = require 'humanize'
LogLines = require '../../collections/LogLines'

{ connect } = require 'react-redux'
{ taskGroupTop, taskGroupBottom } = require '../../actions/log'

scrollThreshold = 100

sum = (numbers) ->
  total = 0
  for n in numbers
    total += n
  total

class LogLines extends React.Component
  @propTypes:
    onEnterTop: React.PropTypes.func.isRequired
    onEnterBottom: React.PropTypes.func.isRequired
    onLeaveTop: React.PropTypes.func.isRequired
    onLeaveBottom: React.PropTypes.func.isRequired

    taskGroupId: React.PropTypes.number.isRequired
    logLines: React.PropTypes.array.isRequired

    initialDataLoaded: React.PropTypes.bool.isRequired
    reachedStartOfFile: React.PropTypes.bool.isRequired
    reachedEndOfFile: React.PropTypes.bool.isRequired
    bytesRemainingBefore: React.PropTypes.number.isRequired
    bytesRemainingAfter: React.PropTypes.number.isRequired
    activeColor: React.PropTypes.string.isRequired

  constructor: (props) ->
    super(props)
    @state = {
      atTop: false
      atBottom: false
    }

  componentDidMount: ->
    window.addEventListener 'resize', @handleScroll

  componentWillUnmount: ->
    window.removeEventListener 'resize', @handleScroll

  componentWillUpdate: ->
    @shouldScrollToBottom = @refs.tailContents.scrollTop + @refs.tailContents.offsetHeight is @refs.tailContents.scrollHeight

  componentDidUpdate: (prevProps, prevState) ->
    if @shouldScrollToBottom
      @refs.tailContents.scrollTop = @refs.tailContents.scrollHeight

    if prevState.atTop != @state.atTop
      if @state.atTop
        @props.onEnterTop(@props.taskGroupId)
      else
        @props.onLeaveTop(@props.taskGroupId)
    if prevState.atBottom != @state.atBottom
      if @state.atBottom
        @props.onEnterBottom(@props.taskGroupId)
      else
        @props.onLeaveBottom(@props.taskGroupId)

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
    newState = {}
    if @state.atTop and @refs.tailContents.scrollTop > @props.scrollThreshold
      newState.atTop = false
      @props.onLeaveTop(@props.taskGroupId)
    else if not @state.atTop and @refs.tailContents.scrollTop < @props.scrollThreshold
      newState.atTop = true
      @props.onEnterTop(@props.taskGroupId)

    if @state.atBottom and (@refs.tailContents.scrollTop + @refs.tailContents.clientHeight) > (@refs.tailContents.scrollHeight - scrollThreshold)
      newState.atBottom = false
      @props.onLeaveBottom(@props.taskGroupId)
    else if not @state.atBottom and (@refs.tailContents.scrollTop + @refs.tailContents.clientHeight) < (@refs.tailContents.scrollHeight - scrollThreshold)
      newState.atBottom = true
      @props.onEnterBottom(@props.taskGroupId)

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
  activeColor: state.activeColor
  initialDataLoaded: _.all(_.pluck(tasks, 'initialDataLoaded'))
  reachedStartOfFile: _.all(tasks.map (task) -> task.minOffset is 0)
  reachedEndOfFile: _.all(tasks.map (task) -> task.maxOffset >= task.filesize)
  bytesRemainingBefore: sum(_.pluck(tasks, 'minOffset'))
  bytesRemainingAfter: sum(tasks.map (task) -> Math.max(task.filesize - task.maxOffset, 0))

mapDispatchToProps = (dispatch, ownProps) ->
  onEnterTop: -> dispatch(taskGroupTop(ownProps.taskGroupId, true))
  onLeaveTop: -> dispatch(taskGroupTop(ownProps.taskGroupId, false))
  onEnterBottom: -> dispatch(taskGroupBottom(ownProps.taskGroupId, true))
  onLeaveBottom: -> dispatch(taskGroupBottom(ownProps.taskGroupId, false))

module.exports = connect(mapStateToProps, mapDispatchToProps)(LogLines)
