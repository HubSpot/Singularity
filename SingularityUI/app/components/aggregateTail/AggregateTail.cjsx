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

  getColumnWidth: ->
    instances = Object.keys(@props.logLines).length
    if instances is 1
      return 12
    else if instances in [2, 4]
      return 6
    else if instances in [3, 5, 6]
      return 4

  getRowType: ->
    if Object.keys(@props.logLines).length > 3 then 'tail-row-half' else 'tail-row'

  renderIndividualTails: ->
    Object.keys(@props.logLines).reverse().map (taskId, i) =>
      <div key={taskId} id="tail-#{taskId}" className="col-md-#{@getColumnWidth()} tail-column">
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
      <div className="row #{@getRowType()}">
        {@renderIndividualTails()}
      </div>
    </div>

module.exports = AggregateTail
