Helpers = require '../helpers'  
EmptyTableMsg = require '../lib/EmptyTableMsg'

StarredRequestsTable = React.createClass
  
  displayName: 'starredRequestsTable'

  getInitialState: ->
    return{
      sortedAttribute: ''
    }

  getDefaultProps: ->
    starredRequests: []

  handleUnstar: (e) ->
    id = $(e.currentTarget).data('id')
    @props.unstar id

  handleSort: (e) ->
    attribute = $(e.currentTarget).data('sort-attribute')

    @setState({
      sortedAttribute: attribute
    })

    @props.sortStarredRequests attribute

  render: ->
    if @props.starredRequests.length is 0
      return (
        <EmptyTableMsg msg='No starred Requests' />
      )
    
    tbody = @props.starredRequests.map (request) =>
      
      link = "#{config.appRoot}/request/#{request.id}"

      return(
        <tr key={request.id}>
          <td>
            <a className="star" onClick={@handleUnstar} data-id={request.id} data-starred="true">
                <span className="glyphicon glyphicon-star"></span>
            </a>
          </td>
          <td>
            <a href={link}>
                { request.id }
            </a>
          </td>
          <td className="hidden-xs" data-value="">
            <span title="">
                { Helpers.timestampFromNow(request.requestDeployState.activeDeploy?.timestamp) }
            </span>
          </td>
          <td className="visible-lg visible-xl">
            { Helpers.usernameFromEmail(request.requestDeployState.activeDeploy?.user) }
          </td>
          <td className="visible-lg visible-xl">
            { request.instances }
          </td>
        </tr>
      )

    return (
      <table className="table table-striped table-staged">
        <thead>
          <tr>
            <th data-sortable="false"></th>
            <th onClick={@handleSort} data-sorted={@state.sortedAttribute is 'request.id'} data-sort-attribute="request.id">Request</th>
            <th onClick={@handleSort} data-sorted={@state.sortedAttribute is 'requestDeployState.activeDeploy.timestamp'} data-sort-attribute="requestDeployState.activeDeploy.timestamp" className="hidden-xs">Requested</th>
            <th onClick={@handleSort} data-sorted={@state.sortedAttribute is 'requestDeployState.activeDeploy.user'} data-sort-attribute="requestDeployState.activeDeploy.user" className="visible-lg visible-xl">Deploy user</th>
            <th onClick={@handleSort} data-sorted={@state.sortedAttribute is 'request.instances'} data-sort-attribute="request.instances" className="visible-lg visible-xl">Instances</th>
          </tr>
        </thead>
        <tbody>
          {tbody}
        </tbody>
      </table>
    )

module.exports = StarredRequestsTable