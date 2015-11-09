
StatusIndicator = React.createClass

  getClassName: ->
    switch @props.status.toUpperCase()
      when 'RUNNING' then 'bg-info running'
      when 'KILLED' then 'bg-danger'

  render: ->
    <div className="status">
      <div className="indicator #{@getClassName()}"></div>
      {@props.status.toLowerCase()}
    </div>

module.exports = StatusIndicator
