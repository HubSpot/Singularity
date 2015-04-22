Helpers = require '../helpers'  

starredRequestsTable = React.createClass
  
  displayName: 'starredRequestsTable'

  getDefaultProps: ->
    starredRequests: []

  render: ->
    tbody = @props.starredRequests.map (request) =>
      
      link = "#{config.appRoot}/request/#{request.id}"
      return(
        <tr data-request-id="{ request.id }">
            <td>
                <a className="star" data-action="unstar" data-starred="true">
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
      <table className="table table-striped">
        <thead>
          <tr>
            <th data-sortable="false"></th>
            <th data-sort-attribute="request.id">Request</th>
            <th className="hidden-xs" data-sort-attribute="">Requested</th>
            <th className="visible-lg visible-xl" data-sort-attribute="">Deploy user</th>
            <th className="visible-lg visible-xl" data-sort-attribute="">Instances</th>
          </tr>
        </thead>
        <tbody>
          {tbody}
        </tbody>
      </table>
    )

module.exports = starredRequestsTable