
# Can't just extend Teeble.ServerCollection directly due to Mixen bugs :(
class DeployHistory extends Mixen(Teeble.ServerCollection)

    url: ->
        params =
            count: @perPage
            page: @currentPage

        "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/request/#{ @requestId }/deploys?#{ $.param params }"

    model: Backbone.Model
    comparator: undefined

    initialize: (models, { @requestId }) =>
        super

    parse: (requestDeployHistoryObjects) ->
        _.each requestDeployHistoryObjects, (deploy, i) =>
            if deploy.deployResult?
                deploy.deployResult.deployStateHuman = constants.deployStates[deploy.deployResult.deployState]
            deploy.deployId = deploy.deployMarker.deployId
            deploy.timestamp = deploy.deployMarker.timestamp
            deploy.timestampHuman = utils.humanTimeAgo deploy.timestamp
            deploy.user = deploy.deployMarker.user?.split('@')[0] ? '—'

        requestDeployHistoryObjects

module.exports = DeployHistory
