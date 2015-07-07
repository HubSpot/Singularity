UserInfo        = require './UserInfo'
RequestsTotals  = require './RequestsTotals'
StarredRequests = require './StarredRequests'

DashboardMain = React.createClass

  displayName: 'DashboardMain'

  propTypes:
    data: React.PropTypes.object.isRequired
    actions: React.PropTypes.func.isRequired
    
  render: ->
    return (
      <div>
        <UserInfo data={@props.data} />
        <RequestsTotals
          data={@props.data} 
          actions={@props.actions}
        />
        <StarredRequests 
          data={@props.data} 
          actions={@props.actions}
        />
      </div>
    )

module.exports = DashboardMain