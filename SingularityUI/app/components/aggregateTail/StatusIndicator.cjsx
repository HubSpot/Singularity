
StatusIndicator = React.createClass

  getClassName: ->
    switch @props.status?.toUpperCase()
      when 'TASK_RUNNING' then 'bg-info running'
      when 'TASK_KILLED', 'TASK_FINISHED' then 'bg-danger'

  render: ->
    <div className="status">
      <div className="indicator #{@getClassName()}"></div>
      {@props.status?.toLowerCase().replace('_', ' ')}
    </div>

module.exports = StatusIndicator
