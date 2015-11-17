
IndividualHeader = require "./IndividualHeader"
Contents = require "./Contents"

TaskHistory = require '../../models/TaskHistory'
LogLines = require '../../collections/LogLines'

InterleavedTail = React.createClass
  mixins: [Backbone.React.Component.mixin]

  # ============================================================================
  # Lifecycle Methods                                                          |
  # ============================================================================

  getInitialState: ->
    @state =
      mergedLines = []

  componentWillMount: ->
    # Get the task info
    @task = new TaskHistory {taskId: @props.taskId}

    collections = {}
    models = {}

    for logLines in @props.logLines
      collections[logLines.taskId] = logLines

    models.ajaxError = @props.ajaxErrors[0]

    Backbone.React.Component.mixin.on(@, {
      models: models
      collections: collections
    });

  componentDidMount: ->
    if @props.offset?
      for logLines in @props.logLines
        logLines.fetchOffset(@props.offset)
    else
      for logLines in @props.logLines
        logLines.fetchInitialData().done =>
          @setState
            mergedLines: LogLines.merge @props.viewingInstances.map (taskId) => @state[taskId]

  componentWillUnmount: ->
    Backbone.React.Component.mixin.off(@)

  # ============================================================================
  # Event Handlers                                                             |
  # ============================================================================

  moreToFetch: ->
    @props.logLines.state.get('moreToFetch')

  fetchNext: ->
    for logLines in @props.logLines
      @props.logLines.fetchNext

  fetchPrevious: (callback) ->
    for logLines in @props.logLines
      @prevLines = logLines.toJSON().length
      _.defer( =>
        logLines.fetchPrevious().done =>
          newLines = logLines.toJSON().length - @prevLines
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
      <Contents
        ref="contents"
        requestId={@props.requestId}
        taskId={@props.taskId}
        logLines={@state.mergedLines}
        ajaxError={@state.ajaxError}
        offset={@props.offset}
        fetchNext={@fetchNext}
        fetchPrevious={@fetchPrevious}
        taskState={''}
        moreToFetch={@moreToFetch}
        activeColor={@props.activeColor} />
    </div>

module.exports = InterleavedTail
