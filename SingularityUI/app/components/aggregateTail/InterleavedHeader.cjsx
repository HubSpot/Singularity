
ColorLegend = require './ColorLegend'

InterleavedHeader = React.createClass

  getInitialState: ->
    @state =
      showLegend: false

  toggleLegend: ->
    @setState
      showLegend: !@state.showLegend

  renderLegend: ->
    if @state.showLegend
      <ColorLegend />

  render: ->
    <div className="individual-header">
      <span className="instance-link">Multiple Tasks</span>
      <span className="right-buttons">
        <a className="action-link" onClick={@toggleLegend}><span className="glyphicon glyphicon-menu-hamburger"></span></a>
      </span>
      {@renderLegend()}
    </div>

module.exports = InterleavedHeader
