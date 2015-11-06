
IndividualHeader = React.createClass

  render: ->
    <div className="individual-header">
      <a className="action-link">⨉</a>
      <a href="#{config.appRoot}/task/#{@props.taskId}">Instance {@props.instanceNumber}</a>
    </div>

module.exports = IndividualHeader
