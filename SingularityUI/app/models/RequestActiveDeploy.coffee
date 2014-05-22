Model = require './model'

class RequestActiveDeploy extends Model

    url: -> "#{ window.singularity.config.apiBase }/history/request/#{ @requestId }/deploy/#{ @deployId }"

    initialize: (models, { @requestId, @deployId }) =>

    parse: (deployObject) ->
        deployObject.JSONString = utils.stringJSON deployObject
        if deployObject.deployResult
            deployObject.deployResult.deployStateHuman = constants.deployStates[deployObject.deployResult.deployState]
            deployObject.deployResult.timestampHuman = utils.humanTimeAgo deployObject.deployResult.timestamp
        app.allDeploys["#{ deployObject.deploy.requestId }-#{ deployObject.deploy.id }"] = deployObject
        deployObject

module.exports = RequestActiveDeploy