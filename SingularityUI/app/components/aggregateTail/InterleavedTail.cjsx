
IndividualHeader = require "./IndividualHeader"
Contents = require "./Contents"

TaskHistory = require '../../models/TaskHistory'

InterleavedTail = React.createClass
  mixins: [Backbone.React.Component.mixin]

  # ============================================================================
  # Lifecycle Methods                                                          |
  # ============================================================================

  componentWillMount: ->
    # Get the task info
    @task = new TaskHistory {taskId: @props.taskId}
    @startTaskStatusPoll()

    for logLines in @props.logLines
      Backbone.React.Component.mixin.on(@, {
        collections: {
          logLines: logLines
        }
      });
    for ajaxError in @props.ajaxErrors
      Backbone.React.Component.mixin.on(@, {
        models: {
          ajaxError: ajaxError
        }
      });

    Backbone.React.Component.mixin.on(@, {
      models: {
        task: @task
      }
    });

  componentDidMount: ->
    if @props.offset?
        @props.logLines.fetchOffset(@props.offset)
    else
        @props.logLines.fetchInitialData()

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

  fetchNext: ->
    _.defer(@props.logLines.fetchNext)

  fetchPrevious: (callback) ->
    @prevLines = @props.logLines.toJSON().length
    _.defer( =>
      @props.logLines.fetchPrevious().done =>
        newLines = @props.logLines.toJSON().length - @prevLines
        if newLines > 0
          @scrollToLine(newLines)
        callback()
    )

  scrollToLine: (line) ->
    @refs.contents.scrollToLine(line)

  scrollToTop: ->
    @refs.contents.stopTailingPoll()
    if @props.logLines.getMinOffset() is 0
      @refs.contents.scrollToTop()
    else
      @props.logLines.reset()
      @props.logLines.fetchFromStart().done @refs.contents.scrollToTop

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
        taskState={_.last(@state.task.taskUpdates)?.taskState} />
      <Contents
        ref="contents"
        requestId={@props.requestId}
        taskId={@props.taskId}
        logLines={@state.logLines}
        ajaxError={@state.ajaxError}
        offset={@props.offset}
        fetchNext={@fetchNext}
        fetchPrevious={@fetchPrevious}
        taskState={_.last(@state.task.taskUpdates)?.taskState}
        moreToFetch={@moreToFetch}
        activeColor={@props.activeColor} />
    </div>

module.exports = InterleavedTail
