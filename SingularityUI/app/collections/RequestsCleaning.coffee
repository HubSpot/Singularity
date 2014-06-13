Requests = require './Requests'

class RequestsCleaning extends Requests

    url: "#{ config.apiRoot }/requests/queued/cleanup"

    parse: (requests) ->
        _.each requests, (request, i) ->
            request.displayState = constants.requestStates.CLEANUP
            request.JSONString = utils.stringJSON request
            request.id = request.requestId
            request.cleanupType = constants.requestCleanupTypes[request.cleanupType]
            request.timestampHuman = utils.humanTimeAgo request.timestamp
            requests[i] = request
            app.allRequests[request.id] = request

        requests

module.exports = RequestsCleaning