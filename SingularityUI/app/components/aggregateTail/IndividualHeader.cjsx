React = require 'react'
StatusIndicator = require './StatusIndicator'

IndividualHeader = React.createClass

  componentDidUpdate: (prevProps, prevState) ->
    target = ReactDOM.findDOMNode(@refs.ttTarget)
    if @props.task.task?.taskId? and target
      $(target).tooltip(container: 'body', template: '<div class="tooltip tailer-tooltip" role="tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner"></div></div>')

    headerTarget = ReactDOM.findDOMNode(@refs.headerTarget)
    if @props.taskState and @props.taskState != prevProps.taskState and headerTarget
      if @props.taskState in ['TASK_KILLED', 'TASK_FAILED', 'TASK_LOST']
        $(headerTarget).addClass('status-changed-stopped')
      else if @props.taskState is 'TASK_FINISHED'
        $(headerTarget).addClass('status-changed-finished')

  componentWillUnmount: () ->
    target = ReactDOM.findDOMNode(@refs.ttTarget)
    if @props.task.task?.taskId? and target
      $(target).tooltip('destroy')

  getTooltipText: ->
    task = @props.task
    "Deploy ID: #{task.task?.taskId?.deployId or ''}\nHost: #{task.task?.taskId?.host or ''}"

  renderClose: ->
    if @props.onlyTask
      return null
    <a className="action-link" onClick={@props.closeTail} title="Close Tail"><span className="glyphicon glyphicon-remove"></span></a>

  renderExpand: ->
    if @props.onlyTask
      return null
    <a className="action-link" onClick={@props.expandTail} title="Show Only this Tail"><span className="glyphicon glyphicon-resize-full"></span></a>

  render: ->
    <div ref="headerTarget" className="individual-header">
      {@renderClose()}
      {@renderExpand()}
      <div ref="ttTarget" className="width-constrained" data-toggle="tooltip" data-placement="bottom" title={@getTooltipText()}>
        <a className="instance-link" href="#{config.appRoot}/task/#{@props.taskId}">{if @props.instanceNumber then "Instance #{@props.instanceNumber}" else @props.taskId}</a>
      </div>
      <span><StatusIndicator status={@props.taskState}/></span>
      <span className="right-buttons">
        <a className="action-link" onClick={@props.scrollToBottom} title="Scroll to Bottom"><span className="glyphicon glyphicon-chevron-down"></span></a>
        <a className="action-link" onClick={@props.scrollToTop} title="Scroll to Top"><span className="glyphicon glyphicon-chevron-up"></span></a>
      </span>
    </div>

module.exports = IndividualHeader
