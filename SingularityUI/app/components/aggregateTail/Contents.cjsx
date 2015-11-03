LogLine = require './LogLine'
Loader = require './Loader'

Contents = React.createClass

  # ============================================================================
  # Lifecycle Methods                                                          |
  # ============================================================================

  getInitialState: ->
    @state =
      contentsHeight: Math.max(document.documentElement.clientHeight, window.innerHeight || 0) - 180
      isLoading: false
      loadingText: ''

  componentWillMount: ->
    $(window).on 'resize orientationChange', @handleResize

  componentDidMount: ->
    @scrollNode = @refs.scrollContainer.getDOMNode()

  componentDidUpdate: (prevProps, prevState) ->
    # If loading without an offset, start tailing immediately
    if !@props.offset and @props.logLines.length > 0 and prevProps.logLines.length is 0
      @scrollToBottom()
    else if @tailingPoll
      @scrollToBottom()

  componentWillUnmount: ->
    $(window).off 'resize orientationChange', @handleResize

  # ============================================================================
  # Event Handlers                                                             |
  # ============================================================================

  handleResize: ->
    @setState
      contentsHeight: Math.max(document.documentElement.clientHeight, window.innerHeight || 0) - 180

  handleScroll: (node) ->
    # Are we at the bottom?
    if $(node).scrollTop() + $(node).innerHeight() >= node.scrollHeight
        @startTailingPoll(node)
    # Or the top?
    else if $(node).scrollTop() is 0
        @props.fetchPrevious(node.scrollHeight)
    else
      @stopTailingPoll()

  startTailingPoll: ->
    # Make sure there isn't one already running
    @stopTailingPoll()

    @setState
      isLoading: true
      loadingText: 'Tailing...'
    @props.fetchNext()
    @tailingPoll = setInterval @props.fetchNext, 1000

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
    if @props.ajaxError.get("present")
      <div className="lines-wrapper">
          <div className="empty-table-message">
              <p>{@props.ajaxError.message}</p>
          </div>
      </div>

  renderLines: ->
    if @props.logLines
      @props.logLines.map((l) =>
        <LogLine content={l.data} offset={l.offset} key={l.offset} highlighted={l.offset is parseInt @props.offset} />
      )

  render: ->
    <div className="contents-container">
      <div className="tail-contents">
        {@renderError()}
        <Infinite
          ref="scrollContainer"
          className="infinite"
          containerHeight={@state.contentsHeight}
          elementHeight={20}
          handleScroll={_.throttle @handleScroll, 200}>
          {@renderLines()}
        </Infinite>
      </div>
      <Loader isVisable={@state.isLoading} text={@state.loadingText} />
    </div>

  # ============================================================================
  # Utility Methods                                                            |
  # ============================================================================

  setScrollHeight: (height) ->
    $(@scrollNode).scrollTop(height);

  scrollToTop: ->
    @setState
      isLoading: true
    @stopTailingPoll()
    @props.fetchPrevious().done =>
      @setScrollHeight(0)
      @setState
        isLoading: false

  scrollToBottom: ->
    @setScrollHeight(@scrollNode.scrollHeight)

module.exports = Contents
