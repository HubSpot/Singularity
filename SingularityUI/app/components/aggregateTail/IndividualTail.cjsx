
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

  fetchNext: ->
    _.defer(@props.logLines.fetchNext)

  fetchPrevious: (callback) ->
    @prevLines = @props.logLines.toJSON().length
    _.defer( =>
      @props.logLines.fetchPrevious().done =>
        newLines = @props.logLines.toJSON().length - @prevLines
        console.log 'new', newLines
        if newLines > 0
          @scrollToLine(newLines)
        callback()
    )

  scrollToLine: (line) ->
    @refs.contents.scrollToLine(line)

  scrollToTop: ->
    if @props.logLines.getMinOffset() is 0
      @refs.contents.scrollToTop()
    else
      @props.logLines.reset()
      @props.logLines.fetchFromStart().done @refs.contents.scrollToTop

  scrollToBottom: ->
    if @props.logLines.state.get('moreToFetch') is true
      @props.logLines.reset()
      @props.logLines.fetchInitialData().done @refs.contents.scrollToBottom
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
        initialScroll={@state.initialScroll} />
    </div>

module.exports = IndividualTail
