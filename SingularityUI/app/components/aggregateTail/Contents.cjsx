LogLine = require './LogLine'

Contents = React.createClass

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
    h = Math.max(document.documentElement.clientHeight, window.innerHeight || 0) - 180
    console.log h
    <div className="contents-container">
      <div className="tail-indicator">
          <div className="page-loader centered"></div>
          <span>Tailing</span>
      </div>

      <div className="tail-contents">
          <div className="tail-fetching-start">
              fetching more lines <div className="page-loader small"></div>
          </div>
          <div className="lines-wrapper">
              {@renderError()}
              <Infinite className="infinite" containerHeight={h} elementHeight={20}>
                {@renderLines()}
              </Infinite>
          </div>
          <div className="tail-fetching-end">
              fetching more lines <div className="page-loader small"></div>
          </div>
      </div>
    </div>

module.exports = Contents
