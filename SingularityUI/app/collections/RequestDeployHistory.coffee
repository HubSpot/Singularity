PaginableCollection = require './PaginableCollection'

class DeployHistory extends PaginableCollection

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/deploys"

    model: Backbone.Model
    comparator: undefined

    initialize: (models, { @requestId }) =>

    parse: (requestDeployHistoryObjects) ->
        for deploy in requestDeployHistoryObjects
            if deploy.deployResult?
                deploy.deployResult.deployStateHuman = constants.deployStates[deploy.deployResult.deployState]
            deploy.deployId = deploy.deployMarker.deployId
            deploy.timestamp = deploy.deployMarker.timestamp
            deploy.timestampHuman = utils.humanTimeAgo deploy.timestamp
            deploy.user = deploy.deployMarker.user?.split('@')[0] ? '—'

        requestDeployHistoryObjects

module.exports = DeployHistory
