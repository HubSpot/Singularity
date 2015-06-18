UserInfo = require './UserInfo'
Requests = require './Requests'

DashboardMain = React.createClass

  displayName: 'DashboardMain'

  propTypes:
    data: React.PropTypes.object.isRequired
    actions: React.PropTypes.func.isRequired
    
  render: ->
    return (
      <div>
        <UserInfo data={@props.data} />
        <Requests 
          data={@props.data} 
          actions={@props.actions}
        />
      </div>
    )

module.exports = DashboardMain