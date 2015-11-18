
InterleavedHeader = require "./InterleavedHeader"
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
          @mergeLines()

  componentWillUnmount: ->
    Backbone.React.Component.mixin.off(@)

  # ============================================================================
  # Event Handlers                                                             |
  # ============================================================================

  mergeLines: ->
    @setState
      mergedLines: LogLines.merge @props.viewingInstances.map (taskId) => @state[taskId]

  moreToFetch: ->
    _.some(@props.logLines, (logLines) =>
      logLines.state.get('moreToFetch')
    )

  fetchNext: ->
    promises = []
    for logLines in @props.logLines
      promises.push(logLines.fetchNext())
    Promise.all(promises).then =>
      @mergeLines()


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

  taskIdToColorMap: (logLines) ->
    if !logLines
      return {}

    map = {}
    taskIds = _.uniq(logLines.map((line) =>
      line.taskId
    ))
    if taskIds.length is 1
      map[taskIds[0]] = 'hsla(0, 0, 0, 0)'
    else
      interval = 360 / taskIds.length
      colors = taskIds.map (taskId, i) =>
        "hsla(#{interval * i}, 100%, 50%, 0.1)"
      for taskId, i in taskIds
        map[taskId] = colors[i]
    map

  render: ->
    <div>
      <InterleavedHeader
        colors={@taskIdToColorMap(@state.mergedLines)}
      />
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
        activeColor={@props.activeColor}
        colorMap={@taskIdToColorMap} />
    </div>

module.exports = InterleavedTail
