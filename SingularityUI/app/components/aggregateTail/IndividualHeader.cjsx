
StatusIndicator = require './StatusIndicator'

IndividualHeader = React.createClass

  render: ->
    <div className="individual-header">
      <a className="action-link" onClick={@props.closeTail}>⨉</a>
      <div className="width-constrained">
        <a className="instance-link" href="#{config.appRoot}/task/#{@props.taskId}">{if @props.instanceNumber then "Instance #{@props.instanceNumber}" else @props.taskId}</a>
      </div>
      <span><StatusIndicator status={@props.taskState}/></span>
      <span className="right-buttons">
        <a className="action-link" onClick={@props.scrollToBottom}><span className="glyphicon glyphicon-chevron-down"></span></a>
        <a className="action-link" onClick={@props.scrollToTop}><span className="glyphicon glyphicon-chevron-up"></span></a>
      </span>
    </div>

module.exports = IndividualHeader
