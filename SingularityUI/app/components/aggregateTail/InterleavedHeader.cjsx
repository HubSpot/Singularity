React = require 'react'
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
      <ColorLegend colors={@props.colors}/>

  render: ->
    <div className="individual-header">
      <span className="instance-link">Viewing {@props.numTasks} Tasks</span>
      <span className="right-buttons">
        <a className="action-link" onClick={@toggleLegend}><span className="glyphicon glyphicon-menu-hamburger"></span></a>
      </span>
      {@renderLegend()}
    </div>

module.exports = InterleavedHeader
