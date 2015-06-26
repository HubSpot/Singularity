TableContainer = require '../containers/TableContainer'
Helpers = require '../utils/helpers'  
EmptyTableMsg = require '../lib/EmptyTableMsg'
StarItem = require '../lib/StarItem'
Table = ReactBootstrap.Table


StarredRequestsTable = React.createClass
  
  displayName: 'StarredRequestsTable'

  getDefaultProps: ->
    starredItems: []


  render: ->

    attribute =
      id: 'request.id'
      timestamp: 'requestDeployState.activeDeploy.timestamp'
      user: 'requestDeployState.activeDeploy.user'
      instances: 'request.Instances'

    dateRequested = (request) ->
      if request?.requestDeployState?.activeDeploy?.timestamp?
        Helpers.timestampFromNow(request?.requestDeployState?.activeDeploy?.timestamp)

    deloyUser = (request) ->
      if request.requestDeployState?.activeDeploy?.user?
        Helpers.usernameFromEmail(request.requestDeployState.activeDeploy.user)

    if @props.data.starredItems.length is 0
      return <EmptyTableMsg msg='No starred Requests' />

    tbody = @props.data.starredItems.map (request) =>
      
      link = "#{config.appRoot}/request/#{request.id}"

      return(
        <tr key={request.id}>
          <td>
            <StarItem id={request.id} active='true' clickEvent={@props.handleUnstar} />
          </td>
          <td>
            <a href={link}>
                { request.id }
            </a>
          </td>
          <td className="hidden-xs" data-value="">
            <span title="">
                { dateRequested(request) }
            </span>
          </td>
          <td className="visible-lg visible-xl">
            { deloyUser(request) }
          </td>
          <td className="visible-lg visible-xl">
            { request.instances }
          </td>
        </tr>
      )

    return (
      <Table striped className="table-staged">
        <thead>
          <tr>
            <th data-sortable="false"></th>
            <th onClick={@props.handleSort} data-sorted={@props.sortedAttribute is attribute.id} data-sort-attribute={attribute.id}>
              Request
              { @props.sortDirection(attribute.id) }
            </th>
            <th onClick={@props.handleSort} data-sorted={@props.sortedAttribute is attribute.timestamp} data-sort-attribute={attribute.timestamp} className="hidden-xs">
              Requested 
              { @props.sortDirection(attribute.timestamp) }
            </th>
            <th onClick={@props.handleSort} data-sorted={@props.sortedAttribute is attribute.user} data-sort-attribute={attribute.user} className="visible-lg visible-xl">
              Deploy user
              { @props.sortDirection(attribute.user) }
            </th>
            <th onClick={@props.handleSort} data-sorted={@props.sortedAttribute is attribute.instances} data-sort-attribute={attribute.instances} className="visible-lg visible-xl">
              Instances 
              { @props.sortDirection(attribute.instances) }
            </th>
          </tr>
        </thead>
        <tbody>
          {tbody}
        </tbody>
      </Table>
    )

module.exports = TableContainer(StarredRequestsTable)