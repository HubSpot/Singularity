# BackboneReact = require "backbone-react-component"

IndividualTail = require './IndividualTail'

AggregateTail = React.createClass

  renderIndividualTails: ->
    Object.keys(@props.logLines).map (taskId) =>
      <div key={taskId} id="tail-#{taskId}" className="col-md-6 tail-column">
        <IndividualTail
          path={@props.path}
          requestId={@props.requestId}
          taskId={taskId}
          offset={@props.offset}
          logLines={@props.logLines[taskId]}
          ajaxError={@props.ajaxError[taskId]}
          activeTasks={@props.activeTasks} />
      </div>

  render: ->
    <div className="row tail-row">
      {@renderIndividualTails()}
    </div>

module.exports = AggregateTail
