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

  componentDidMount: ->
    @scrollNode = ReactDOM.findDOMNode(@refs.scrollContainer)
    @currentOffset = parseInt @props.offset

  componentDidUpdate: (prevProps, prevState) ->
    # Scroll to the appropriate place
    if @state.linesToRender.length > 0 and prevState.linesToRender.length is 0
      if !@props.offset
        @scrollToBottom()
      else
        @scrollToLine(0)
    # if $(@scrollNode).scrollTop() is 0
    #   # console.log @props.isMoreToFetchAtBeginning()
    #   # @scrollToLine(1)
    else if @tailingPoll
      @scrollToBottom()

    # Start tailing automatically if we can't scroll
    if @props.taskState in Utils.TERMINAL_TASK_STATES
      @stopTailingPoll()
    else if (0 < $('.line').length * 20 <= @state.contentsHeight) and !@tailingPoll
      @startTailingPoll()

    # Update our loglines components only if needed
    if prevProps.logLines.length isnt @props.logLines.length
      @setState
        linesToRender: @renderLines()

  # ============================================================================
  # Event Handlers                                                             |
  # ============================================================================

  handleScroll: ->
    node = @scrollNode
    # Are we at the bottom?
    if $(node).scrollTop() + $(node).innerHeight() >= node.scrollHeight
      @startTailingPoll(node)
    # Or the top?
    else if $(node).scrollTop() is 0
      @stopTailingPoll()
      @props.fetchPrevious()
    else
      @stopTailingPoll()

  handleHighlight: (e) ->
    @currentOffset = parseInt $(e.target).attr 'data-offset'
    @setState
      linesToRender: @renderLines()

  startTailingPoll: ->
    # Make sure there isn't one already running
    @stopTailingPoll()
    @setState
      isLoading: true
      loadingText: 'Tailing'
    # @props.fetchNext()
    @tailingPoll = setInterval =>
      @props.fetchNext()
    , 2000

  stopTailingPoll: ->
    if @tailingPoll
      clearInterval @tailingPoll
      @tailingPoll = null
      @setState
        isLoading: false
        loadingText: ''

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

  renderLines: ->
    if @props.logLines
      @props.logLines.map((l, i) =>
        <LogLine
          content={l.data}
          offset={l.offset}
          key={i}
          index={i}
          highlighted={l.offset is @currentOffset}
          highlight={@handleHighlight} />
      )

  lineRenderer: (index, key) ->
    @state.linesToRender[index]

  getLineHeight: (index) ->
    if index is 0
      return 40
    else
      return 20

  render: ->
    <div className="contents-container">
      <div className="tail-contents" ref="scrollContainer" onScroll={_.throttle @handleScroll, 200}>
        {@renderError()}
        <ReactList
          className="infinite"
          ref="lines"
          itemRenderer={@lineRenderer}
          itemSizeGetter={@getLineHeight}
          length={@state.linesToRender.length}
          type="uniform">
        </ReactList>
      </div>
      <Loader isVisable={@state.isLoading} text={@state.loadingText} />
    </div>

  # ============================================================================
  # Utility Methods                                                            |
  # ============================================================================

  scrollToLine: (line) ->
    console.log 'scrollto ' + line
    # console.log 'set', height, arguments.callee.caller
    @refs.lines.scrollTo(line)

  scrollToTop: ->
    console.log 'top'
    @stopTailingPoll()
    @setState
      isLoading: true
    @props.fetchFromStart().done =>
      @refs.lines.scrollTo(0)
      @setState
        isLoading: false

  scrollToBottom: ->
    console.log 'bot'
    # console.log @state.linesToRender.length
    @refs.lines.scrollTo(@state.linesToRender.length)

module.exports = Contents
