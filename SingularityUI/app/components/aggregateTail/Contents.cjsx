LogLine = require './LogLine'
Loader = require './Loader'

Contents = React.createClass

  getInitialState: ->
    @state =
      contentsHeight: Math.max(document.documentElement.clientHeight, window.innerHeight || 0) - 180
      isLoading: false
      loadingText: ''

  componentWillMount: ->
    $(window).on 'resize orientationChange', @handleResize

  componentWillUnmount: ->
    $(window).off 'resize orientationChange', @handleResize

  handleResize: ->
    @setState
      contentsHeight: Math.max(document.documentElement.clientHeight, window.innerHeight || 0) - 180

  handleScroll: (node) ->
    # Are we at the bottom?
    if $(node).scrollTop() + $(node).innerHeight() >= node.scrollHeight
        @scrollNode = node
        @startTailingPoll(node)
    # Or the top?
    else if $(node).scrollTop() is 0
        console.log 'top reached'
    else
      @stopTailingPoll()

  startTailingPoll: (scrollNode) ->
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

  componentDidUpdate: (prevProps, prevState) ->
    if @tailingPoll
      $(@scrollNode).scrollTop(@scrollNode.scrollHeight);

module.exports = Contents
