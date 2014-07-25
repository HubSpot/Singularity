Collection = require './collection'

class RequestsCleaning extends Collection

    url: "#{ config.apiRoot }/requests/queued/cleanup"

    parse: (requests) ->
        for request in requests
            request.displayState = constants.requestStates.CLEANUP
            request.originalObject = _.clone request
            request.id = request.requestId
            request.cleanupType = constants.requestCleanupTypes[request.cleanupType]
            request.timestampHuman = utils.humanTimeAgo request.timestamp

        requests

module.exports = RequestsCleaning