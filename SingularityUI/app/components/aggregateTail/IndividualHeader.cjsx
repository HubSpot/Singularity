
IndividualHeader = React.createClass

  render: ->
    <div className="individual-header">
      <a className="action-link">⨉</a>
      <a className="instance-link" href="#{config.appRoot}/task/#{@props.taskId}">Instance {@props.instanceNumber}</a>
      <span className="right-buttons">
        <a className="action-link" onClick={@props.scrollToBottom}><span className="glyphicon glyphicon-chevron-down"></span></a>
        <a className="action-link" onClick={@props.scrollToTop}><span className="glyphicon glyphicon-chevron-up"></span></a>
      </span>
    </div>

module.exports = IndividualHeader
