
ColorLegend = React.createClass

  renderColors: ->
    _.keys(@props.colors).map (taskId) =>
      <li key={taskId}>
        {@props.colors[taskId]} {taskId}
      </li>

  render: ->
    <div className="legend">
      <ul>
        {@renderColors()}
      </ul>
    </div>

module.exports = ColorLegend
