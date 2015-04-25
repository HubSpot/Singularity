UserInfo = require './UserInfo'
Requests = require './Requests'

DashboardMain = React.createClass

  displayName: 'DashboardMain'
  
  render: ->
    return (
      <div>
        <UserInfo 
          user={@props.user}
          refresh={@props.refresh}
        />
        <Requests 
          requestTotals={@props.totals} 
          starredRequests={@props.starredRequests}
          username={@props.username}
        />
      </div>
    )

module.exports = DashboardMain