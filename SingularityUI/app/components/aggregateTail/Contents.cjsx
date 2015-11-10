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

  componentWillMount: ->
    $(window).on 'resize orientationChange', @handleResize

  componentDidMount: ->
    @scrollNode = @refs.scrollContainer.getDOMNode()
    @currentOffset = parseInt @props.offset
    @handleResize()

  componentDidUpdate: (prevProps, prevState) ->
    # Scroll to the appropriate place
    if @state.linesToRender.length > 0 and prevState.linesToRender.length is 0
      if !@props.offset
        @scrollToBottom()
    if $(@scrollNode).scrollTop() is 0
      @setScrollHeight(20)
    else if @tailingPoll
      @scrollToBottom()

    # Start tailing automatically if we can't scroll
    if @props.taskState in Utils.TERMINAL_TASK_STATES and @tailingPoll
      @stopTailingPoll()
    else if 0 < $('.line').length * 20 <= @state.contentsHeight and !@tailingPoll
      @startTailingPoll()

    # Update our loglines components only if needed
    if prevProps.logLines.length isnt @props.logLines.length
      @setState
        linesToRender: @renderLines()

  componentWillUnmount: ->
    $(window).off 'resize orientationChange', @handleResize

  # ============================================================================
  # Event Handlers                                                             |
  # ============================================================================

  handleResize: ->
    height = $("#tail-#{@props.taskId.replace( /(:|\.|\[|\]|,)/g, "\\$1" )}").height() - 20
    @setState
      contentsHeight: height

  handleScroll: (node) ->
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
      loadingText: 'Tailing...'
    @props.fetchNext()
    @tailingPoll = setInterval =>

      @props.fetchNext()
    , 2000

  stopTailingPoll: ->
    @setState
      isLoading: false
      loadingText: ''
    clearInterval @tailingPoll
    @tailingPoll = null

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

  render: ->
    <div className="contents-container">
      <div className="tail-contents">
        {@renderError()}
        <Infinite
          ref="scrollContainer"
          className="infinite"
          containerHeight={@state.contentsHeight || 1}
          preloadAdditionalHeight={@state.contentsHeight * 2.5}
          elementHeight={20}
          handleScroll={_.throttle @handleScroll, 200}>
          {@state.linesToRender}
        </Infinite>
      </div>
      <Loader isVisable={@state.isLoading} text={@state.loadingText} />
    </div>

  # ============================================================================
  # Utility Methods                                                            |
  # ============================================================================

  setScrollHeight: (height) ->
    # console.log 'set', height, arguments.callee.caller
    $(@scrollNode).scrollTop(height);

  scrollToTop: ->
    @stopTailingPoll()
    @setState
      isLoading: true
    @props.fetchFromStart().done =>
      @setScrollHeight(0)
      @setState
        isLoading: false

  scrollToBottom: ->
    @setScrollHeight(@scrollNode.scrollHeight)

module.exports = Contents
