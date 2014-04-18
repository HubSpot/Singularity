Model = require './model'

class RequestDeployHistory extends Model

    url: -> "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/request/#{ @requestId }/deploys"

    initialize: (models, { @requestId }) =>

    parse: (requestDeployHistoryObjects) ->
        requestDeployHistory = {}
        requestDeployHistory.requestId = @requestId
        requestDeployHistory.deploys = requestDeployHistoryObjects

        _.each requestDeployHistory.deploys, (deploy, i) =>
            if deploy.deployResult?
                deploy.deployResult.deployStateHuman = constants.deployStates[deploy.deployResult.deployState]
            deploy.deployId = deploy.deployMarker.deployId
            deploy.timestamp = deploy.deployMarker.timestamp
            deploy.timestampHuman = utils.humanTimeAgo deploy.timestamp
            deploy.user = deploy.deployMarker.user?.split('@')[0] ? '—'

        _.sortBy requestDeployHistory.deploys, (r) -> r.timestamp
        requestDeployHistory.deploys.reverse()

        requestDeployHistory

module.exports = RequestDeployHistory