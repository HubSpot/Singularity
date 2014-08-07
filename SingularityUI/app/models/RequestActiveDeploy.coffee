Model = require './model'

# Gets info about the active deploy, we only want it for the statistics
class RequestActiveDeploy extends Model

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/deploy/#{ @deployId }"

    initialize: ({@requestId, @deployId}) =>

module.exports = RequestActiveDeploy
