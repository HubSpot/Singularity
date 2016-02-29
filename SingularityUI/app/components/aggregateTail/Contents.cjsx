React = require 'react'
ReactDOM = require 'react-dom'
ReactList = require 'react-list'
LogLine = require './LogLine'
Loader = require './Loader'

Utils = require '../../utils'

Contents = React.createClass

  # ============================================================================
  # Lifecycle Methods                                                          |
  # ============================================================================

  getInitialState: ->
    @state =
      isLoading: false
      loadingText: ''
      linesToRender: []
      loadingFromTop: false

  componentDidMount: ->
    @scrollNode = ReactDOM.findDOMNode(@refs.scrollContainer)
    @currentOffset = parseInt @props.offset
    if @props.taskState not in Utils.TERMINAL_TASK_STATES and not @props.ajaxError.present and not @props.offset
      @startTailingPoll()

  componentDidUpdate: (prevProps, prevState) ->
    if @tailingPoll and not @state.loadingFromTop
      @scrollToBottom()

    # Stop tailing if the task dies
    if @props.taskState in Utils.TERMINAL_TASK_STATES or @props.ajaxError.present
      @stopTailingPoll()

    # Update our loglines components only if needed
    if prevProps.logLines?.length isnt @props.logLines?.length
      @setState
        linesToRender: @renderLines(@props.offset)

  componentWillReceiveProps: (nextProps) ->
    if nextProps.offset isnt @props.offset
      @setState
        linesToRender: @renderLines(nextProps.offset)

  componentWillUnmount: ->
    @stopTailingPoll()

  # ============================================================================
  # Event Handlers                                                             |
  # ============================================================================

  handleScroll: (e) ->
    node = @scrollNode
    # Are we at the bottom?
    if $(node).scrollTop() + $(node).innerHeight() >= node.scrollHeight - 20
      if @props.moreToFetch()
        @props.fetchNext()
      else if @props.taskState not in Utils.TERMINAL_TASK_STATES and @props.logLines.length > 0 and not @state.loadingFromTop
        @startTailingPoll()
    # Or the top?
    else if $(node).scrollTop() is 0
      if not @tailingPoll and @props.logLines[0]?.offset > 0
        @setState
          isLoading: true
          loadingText: 'Fetching'
        @props.fetchPrevious( =>
          @setState
            isLoading: false
            loadingText: ''
        )
    else
      @stopTailingPoll()

  handleKeyDown: (e) ->
    if e.keyCode is 38
      @stopTailingPoll()

  startTailingPoll: ->
    # Make sure there isn't one already running
    @stopTailingPoll()
    @setState
      isLoading: true
      loadingText: 'Tailing'
    @tailingPoll = setInterval =>
      if @props.reachedEndOfFile() and not @props.reachedStartOfFile() and @scrollNode.scrollHeight <= $(@scrollNode).innerHeight()
        @setState
          isLoading: true
          loadingText: 'Loading'
        @props.fetchPrevious( => )
      else
        @setState
          isLoading: true
          loadingText: 'Tailing'
        @props.fetchNext()
    , 2000

  stopTailingPoll: ->
    if @tailingPoll
      clearInterval @tailingPoll
      @tailingPoll = null
      @setState
        isLoading: false
        loadingText: ''
        loadingFromTop: false

  loadFromTop: ->
    # Make sure there isn't one already running
    @stopTailingPoll()
    @setState
      isLoading: true
      loadingText: 'Loading'
      loadingFromTop: true
    @tailingPoll = setInterval =>
      if @state.linesToRender and @refs.lines.getVisibleRange()[1] < @state.linesToRender.length * 2
        @stopTailingPoll()
      else
        @props.fetchNext()
    , 2000

  # ============================================================================
  # Rendering                                                                  |
  # ============================================================================

  renderError: ->
    if @props.ajaxError.present
      <div className="lines-wrapper">
          <div className="empty-table-message">
              <p>{@props.ajaxError.message}</p>
          </div>
      </div>

  renderLines: (offset) ->
    if @props.logLines and @props.logLines.length > 0
      if @props.colorMap
        colors = @props.colorMap(@props.logLines)
      else
        colors = {}
        colors[@props.logLines[0].taskId] = 'hsla(0, 0, 0, 0)'
      @props.logLines.map((l, i) =>
        link = window.location.href.replace(window.location.search, '').replace(window.location.hash, '')
        link += "?taskIds=#{@props.taskId}##{l.offset}"
        isHighlighted = l.offset is offset
        isFirstLine = i is 0
        isLastLine = i is @props.logLines.length - 1
        <LogLine
          content={l.data}
          offset={l.offset}
          key={i}
          index={i}
          isHighlighted={isHighlighted}
          isLastLine={isLastLine}
          isFirstLine={isFirstLine}
          offsetLink={link}
          handleOffsetLink={@props.handleOffsetLink}
          taskId={l.taskId}
          color={colors[l.taskId]}
          search={@props.search} />
      )

  lineRenderer: (index, key) ->
    @state.linesToRender[index]

  getLineHeight: (index) ->
    if index in [0, @state.linesToRender.length]
      return 40
    else
      return 20

  render: ->
    <div className="contents-container">
      <div className="tail-contents #{@props.activeColor}" ref="scrollContainer" onScroll={@handleScroll}  onKeyDown={@handleKeyDown} tabIndex="1">
        {@renderError()}
        <ReactList
          className="infinite"
          ref="lines"
          highlightOffset={@props.offset}
          itemRenderer={@lineRenderer}
          itemSizeGetter={@getLineHeight}
          length={@state.linesToRender?.length || 0}
          type="variable"
          useTranslate3d={true}
          threshold={5000}
          pageSize={10}>
        </ReactList>
      </div>
      <Loader isVisable={@state.isLoading} text={@state.loadingText} />
    </div>

  # ============================================================================
  # Utility Methods                                                            |
  # ============================================================================

  isTailing: ->
    !(_.isNull(@tailingPoll) or _.isUndefined(@tailingPoll))

  scrollToLine: (line) ->
    @refs.lines.scrollTo(line)

  scrollToTop: ->
    @refs.lines.scrollTo(0)

  scrollToBottom: ->
    @refs.lines.scrollTo(@state.linesToRender?.length || 0)

module.exports = Contents
