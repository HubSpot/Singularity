
ColorLegend = React.createClass

  renderColors: ->
    _.keys(@props.colors).map (taskId) =>
      <li key={taskId}>
        <div className="swatch" style={backgroundColor: @props.colors[taskId]}></div>{taskId}
      </li>

  render: ->
    <div className="legend">
      <ul>
        {@renderColors()}
      </ul>
    </div>

module.exports = ColorLegend
