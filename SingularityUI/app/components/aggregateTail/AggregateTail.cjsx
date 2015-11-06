# BackboneReact = require "backbone-react-component"

Header = require './Header'
IndividualTail = require './IndividualTail'

AggregateTail = React.createClass

  scrollAllTop: ->
    for tail of @refs
      @refs[tail].scrollToTop()

  scrollAllBottom: ->
    for tail of @refs
      @refs[tail].scrollToBottom()

  renderIndividualTails: ->
    Object.keys(@props.logLines).map (taskId, i) =>
      <div key={taskId} id="tail-#{taskId}" className="col-md-6 tail-column">
        <IndividualTail
          ref="tail_#{i}"
          path={@props.path}
          requestId={@props.requestId}
          taskId={taskId}
          offset={@props.offset}
          logLines={@props.logLines[taskId]}
          ajaxError={@props.ajaxError[taskId]}
          activeTasks={@props.activeTasks} />
      </div>

  render: ->
    <div>
      <Header
       path={@props.path}
       requestId={@props.requestId}
       scrollToTop={@scrollAllTop}
       scrollToBottom={@scrollAllBottom} />
      <div className="row tail-row">
        {@renderIndividualTails()}
      </div>
    </div>

module.exports = AggregateTail
