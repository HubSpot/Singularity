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

  renderLoader: ->
    <Loader isVisable={@state.isLoading} text={@state.loadingText} />

  render: ->
    <div className="contents-container">
      <div className="tail-indicator">
          <div className="page-loader centered"></div>
          <span>Tailing</span>
      </div>
      <div className="tail-contents">
          <div className="tail-fetching-start">
              fetching more lines <div className="page-loader small"></div>
          </div>
              {@renderError()}
              <Infinite className="infinite" containerHeight={@state.contentsHeight} elementHeight={20}>
                {@renderLines()}
              </Infinite>
          <div className="tail-fetching-end">
              fetching more lines <div className="page-loader small"></div>
          </div>
      </div>
      {@renderLoader()}
    </div>

module.exports = Contents
