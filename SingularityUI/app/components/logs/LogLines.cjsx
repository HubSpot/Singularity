React = require 'react'
ReactDOM = require 'react-dom'
Waypoint = require 'react-waypoint'
LogLine = require './LogLine'
Humanize = require 'humanize'
LogLines = require '../../collections/LogLines'

Utils = require '../../utils'

class LogLines extends React.Component
  @propTypes:
    onEnterTop: React.PropTypes.func.isRequired
    onEnterBottom: React.PropTypes.func.isRequired
    onLeaveTop: React.PropTypes.func.isRequired
    onLeaveBottom: React.PropTypes.func.isRequired
    onPermalinkClick: React.PropTypes.func.isRequired

    taskGroupId: React.PropTypes.number.isRequired
    logLines: React.PropTypes.array.isRequired
    search: React.PropTypes.string.isRequired

    initialDataLoaded: React.PropTypes.bool.isRequired
    reachedStartOfFile: React.PropTypes.bool.isRequired
    reachedEndOfFile: React.PropTypes.bool.isRequired
    bytesRemainingBefore: React.PropTypes.number.isRequired
    bytesRemainingAfter: React.PropTypes.number.isRequired
    permalinkEnabled: React.PropTypes.bool.isRequired
    activeColor: React.PropTypes.string.isRequired

  componentWillUpdate: ->
    @shouldScrollToBottom = @refs.tailContents.scrollTop + @refs.tailContents.offsetHeight is @refs.tailContents.scrollHeight

  componentDidUpdate: ->
    if @shouldScrollToBottom
      @refs.tailContents.scrollTop = @refs.tailContents.scrollHeight

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
        isHighlighted={offset is @props.initialOffset}
        onPermalinkClick={@props.onPermalinkClick}
        taskId={taskId}
        permalinkEnabled={@props.permalinkEnabled}
        search={@props.search} />

  renderLoadingMore: ->
    if @props.initialDataLoaded
      if @props.reachedEndOfFile
        <div>Tailing...</div>
      else
        <div>Loading more... ({Humanize.filesize(@props.bytesRemainingAfter)} remaining)</div>

  handleEnterTop: => @props.onEnterTop(@props.taskGroupId)
  handleEnterBottom: => @props.onEnterBottom(@props.taskGroupId)
  handleLeaveTop: => @props.onLeaveTop(@props.taskGroupId)
  handleLeaveBottom: => @props.onLeaveBottom(@props.taskGroupId)

  render: ->
    <div className="contents-container">
      <div className="tail-contents #{@props.activeColor}" tabIndex="1" ref="tailContents">
        {@renderLoadingPrevious()}
        <Waypoint onEnter={@handleEnterTop} onLeave={@handleLeaveTop} />
        {@renderLogLines()}
        <Waypoint onEnter={@handleEnterBottom} onLeave={@handleLeaveBottom} />
        {@renderLoadingMore()}
      </div>
    </div>

module.exports = LogLines
