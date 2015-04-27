UserInfo = require './UserInfo'
Requests = require './Requests'

DashboardMain = React.createClass

  displayName: 'DashboardMain'

  propTypes:
    user: React.PropTypes.object.isRequired
    username: React.PropTypes.string.isRequired
    totals: React.PropTypes.array.isRequired
    starredRequests: React.PropTypes.array.isRequired
    refresh: React.PropTypes.func.isRequired
    unstar: React.PropTypes.func.isRequired
    sortStarredRequests: React.PropTypes.func.isRequired
    
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
          unstar={@props.unstar}
          sortStarredRequests={@props.sortStarredRequests}
        />
      </div>
    )

module.exports = DashboardMain