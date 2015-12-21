
StatusIndicator = require './StatusIndicator'

IndividualHeader = React.createClass

  componentDidUpdate: (prevProps, prevState) ->
    if @props.task.task?.taskId?
      $('[data-toggle="tooltip"]').tooltip(container: 'body', template: '<div class="tooltip tailer-tooltip" role="tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner"></div></div>')

  getTooltipText: ->
    task = @props.task
    "Deploy ID: #{task.task?.taskId?.deployId or ''}\nHost: #{task.task?.taskId?.host or ''}"

  render: ->
    <div className="individual-header">
      <a className="action-link" onClick={@props.closeTail}><span className="glyphicon glyphicon-remove"></span></a>
      <a className="action-link" onClick={@props.expandTail}><span className="glyphicon glyphicon-resize-full"></span></a>
      <div className="width-constrained" data-toggle="tooltip" data-placement="bottom" title={@getTooltipText()}>
        <a className="instance-link" href="#{config.appRoot}/task/#{@props.taskId}">{if @props.instanceNumber then "Instance #{@props.instanceNumber}" else @props.taskId}</a>
      </div>
      <span><StatusIndicator status={@props.taskState}/></span>
      <span className="right-buttons">
        <a className="action-link" onClick={@props.scrollToBottom}><span className="glyphicon glyphicon-chevron-down"></span></a>
        <a className="action-link" onClick={@props.scrollToTop}><span className="glyphicon glyphicon-chevron-up"></span></a>
      </span>
    </div>

module.exports = IndividualHeader
