Model = require './model'

class RequestActiveDeploy extends Model

    url: -> "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/request/#{ @requestId }/deploy/#{ @deployId }"

    initialize: (models, { @requestId, @deployId }) =>

    parse: (deployObject) ->
        if deployObject.deployResult
            deployObject.deployResult.deployStateHuman = constants.deployStates[deployObject.deployResult.deployState]
            deployObject.deployResult.timestampHuman = utils.humanTimeAgo deployObject.deployResult.timestamp
        deployObject

module.exports = RequestActiveDeploy