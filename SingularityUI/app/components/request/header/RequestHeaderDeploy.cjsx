Helpers = require '../../utils/helpers'

RequestHeaderDeploy = React.createClass

  displayName: 'RequestHeaderDeploy'

  handleDeployHistory: ->
    # To Do:
    # expand-deploy-history on click

  render: ->
    activeDeploy = @props.data.request.activeDeploy

    if activeDeploy
      id = activeDeploy.id
      timestamp = Helpers.timestampFromNow activeDeploy.timestamp
      deployedBy = Helpers.usernameFromEmail(activeDeploy.metadata.deployedBy)

      deployStatus = 
        <div>
          Active deploy <code>{id}</code>
          { if deployedBy then <span> by <strong>{deployedBy}</strong> </span> }
          { if timestamp then <span>&mdash; {timestamp}</span> }
          <a onClick={@handleDeployHistory} className="pull-right">
            Deploy history
          </a>
        </div>

    else
      deployStatus = <span className='text-danger'> No active deploy </span>

    return (
      <div className="well">
        {deployStatus}
      </div>
    )

module.exports = RequestHeaderDeploy