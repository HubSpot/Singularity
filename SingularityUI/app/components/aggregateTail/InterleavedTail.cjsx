
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
      mergedLines: []

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
      # collections: collections
    });

  componentDidMount: ->
    for logLines in @props.logLines
      logLines.reset()

    promises = []
    for logLines in @props.logLines
      promises.push(logLines.fetchInitialData())
    Promise.all(promises).then =>
      @mergeLines(@props.logLines.map((logLines) => logLines.toJSON()))

  componentWillUnmount: ->
    Backbone.React.Component.mixin.off(@)

  # ============================================================================
  # Event Handlers                                                             |
  # ============================================================================

  mergeLines: (lines, beginning = false) ->
    if beginning
      newLines = LogLines.merge(lines).concat(@state.mergedLines)
    else
      newLines = @state.mergedLines.concat(LogLines.merge lines)
    @setState
      mergedLines: newLines

  moreToFetch: ->
    _.some(@props.logLines, (logLines) =>
      logLines.state.get('moreToFetch')
    )

  fetchNext: ->
    promises = []
    oldLineCount = @props.logLines.map (logLines) => {taskId: logLines.taskId, length: logLines.length}
    for logLines in @props.logLines
      promises.push(logLines.fetchNext())

    Promise.all(promises).then =>
      newLineCount = @props.logLines.map (logLines) => {taskId: logLines.taskId, length: logLines.length}
      deltas = newLineCount.map (count) =>
        taskId: count.taskId
        delta: count.length - _.findWhere(oldLineCount, {taskId: count.taskId}).length

      newLines = []
      for delta in deltas
        lines = _.findWhere(@props.logLines, {taskId: delta.taskId}).toJSON()
        slice = lines.slice(lines.length - delta.delta, lines.length)
        newLines.push(slice)

      @mergeLines(newLines)

  fetchPrevious: (callback) ->
    promises = []
    oldLineCount = @props.logLines.map (logLines) => {taskId: logLines.taskId, length: logLines.length}
    for logLines in @props.logLines
      promises.push(logLines.fetchPrevious())

    Promise.all(promises).then =>
      newLineCount = @props.logLines.map (logLines) => {taskId: logLines.taskId, length: logLines.length}
      deltas = newLineCount.map (count) =>
        taskId: count.taskId
        delta: count.length - _.findWhere(oldLineCount, {taskId: count.taskId}).length

      newLines = []
      for delta in deltas
        lines = _.findWhere(@props.logLines, {taskId: delta.taskId}).toJSON()
        slice = lines.slice(0, delta.delta)
        newLines.push(slice)

      @mergeLines(newLines, true)
      totalNew = _.reduce(deltas, (memo, delta) =>
        memo + delta.delta
      , 0)
      if totalNew > 0
        @scrollToLine(totalNew)
      callback()

  scrollToLine: (line) ->
    @refs.contents.scrollToLine(line)

  scrollToTop: ->
    @refs.contents.stopTailingPoll()
    if _.every(@props.logLines, (logLines) => logLines.getMinOffset() is 0)
      @refs.contents.scrollToTop()
    else
      _.each(@props.logLines, (logLines) => logLines.reset())
      promises = []
      _.each(@props.logLines, (logLines) => promises.push(logLines.fetchFromStart()))
      Promise.all(promises).then =>
        @setState
          mergedLines: []
        @mergeLines(@props.logLines.map((logLines) => logLines.toJSON()))
        @refs.contents.scrollToTop

  scrollToBottom: ->
    if _.every(@props.logLines, (logLines) => logLines.state.get('moreToFetch') is true)
      _.each(@props.logLines, (logLines) => logLines.reset())
      promises = []
      for logLines in @props.logLines
        promises.push(logLines.fetchInitialData())
      Promise.all(promises).then =>
        @setState
          mergedLines: []
        @mergeLines(@props.logLines.map((logLines) => logLines.toJSON()))
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
    )).sort()
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
