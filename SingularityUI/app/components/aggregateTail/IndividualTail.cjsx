React = require 'react'
BackboneReactComponent = require 'backbone-react-component'
IndividualHeader = require "./IndividualHeader"
Contents = require "./Contents"

TaskHistory = require '../../models/TaskHistory'

IndividualTail = React.createClass
  mixins: [Backbone.React.Component.mixin]

  # ============================================================================
  # Lifecycle Methods                                                          |
  # ============================================================================

  componentWillMount: ->
    # Get the task info
    @task = new TaskHistory {taskId: @props.taskId}
    @startTaskStatusPoll()

    @props.logLines.grep = @props.search

    # Automatically map backbone collections and models to the state of this component
    Backbone.React.Component.mixin.on(@, {
      collections: {
        logLines: @props.logLines
      },
      models: {
        ajaxError: @props.ajaxError
        task: @task
      }
    });

  componentDidMount: ->
    if @props.offset?
        @props.logLines.fetchOffset(@props.offset)
    else
        @props.logLines.fetchInitialData()

  componentWillReceiveProps: (nextProps) ->
    if nextProps.search isnt @props.search
      @props.logLines.grep = nextProps.search
      @props.logLines.reset()
      @props.logLines.fetchInitialData().done _.delay(@refs.contents.scrollToBottom, 200)

  componentWillUnmount: ->
    Backbone.React.Component.mixin.off(@)
    @stopTaskStatusPoll()

  # ============================================================================
  # Event Handlers                                                             |
  # ============================================================================

  startTaskStatusPoll: ->
    @task.fetch()
    @taskPoll = setInterval =>
      @task.fetch()
    , 5000

  stopTaskStatusPoll: ->
    clearInterval @taskPoll

  moreToFetch: ->
    @props.logLines.state.get('moreToFetch')

  reachedStartOfFile: ->
    @props.logLines.getMinOffset() is 0

  reachedEndOfFile: ->
    @props.logLines.state.get('reachedEndOfFile')

  fetchNext: ->
    _.defer(@props.logLines.fetchNext)

  fetchPrevious: (callback) ->
    @prevLineCount = @props.logLines.length
    _.defer( =>
      xhr = @props.logLines.fetchPrevious()
      if xhr
        xhr.done =>
          newLines = @props.logLines.length - @prevLineCount
          if newLines > 0
            @scrollToLine(newLines)
          callback()
    )

  isTailing: ->
    @refs.contents.isTailing()

  stopTailing: ->
    @refs.contents.stopTailingPoll()

  startTailing: ->
    @refs.contents.startTailingPoll()

  scrollToLine: (line) ->
    @refs.contents.scrollToLine(line)

  scrollToTop: ->
    @refs.contents.stopTailingPoll()
    if @props.logLines.getMinOffset() is 0
      @refs.contents.scrollToTop()
    else
      @props.logLines.reset()
      @props.logLines.fetchFromStart().done =>
        @refs.contents.scrollToTop()
        @refs.contents.loadFromTop()

  scrollToBottom: ->
    if @props.logLines.state.get('moreToFetch') is true
      @props.logLines.reset()
      @props.logLines.fetchInitialData().done _.delay(@refs.contents.scrollToBottom, 200)
    else
      @refs.contents.scrollToBottom()

  # ============================================================================
  # Rendering                                                                  |
  # ============================================================================

  render: ->
    <div>
      <IndividualHeader
        taskId={@props.taskId}
        instanceNumber={@props.instanceNumber}
        scrollToTop={@scrollToTop}
        scrollToBottom={@scrollToBottom}
        closeTail={() => @props.closeTail(@props.taskId)}
        expandTail={() => @props.expandTail(@props.taskId)}
        taskState={_.last(@state.task.taskUpdates)?.taskState}
        task={@state.task}
        onlyTask={@props.onlyTask} />
      <Contents
        ref="contents"
        requestId={@props.requestId}
        taskId={@props.taskId}
        logLines={@state.logLines}
        ajaxError={@state.ajaxError}
        offset={@props.offset}
        handleOffsetLink={@props.handleOffsetLink}
        fetchNext={@fetchNext}
        fetchPrevious={@fetchPrevious}
        taskState={_.last(@state.task.taskUpdates)?.taskState}
        moreToFetch={@moreToFetch}
        reachedStartOfFile={@reachedStartOfFile}
        reachedEndOfFile={@reachedEndOfFile}
        activeColor={@props.activeColor}
        search={@props.search} />
    </div>

module.exports = IndividualTail
