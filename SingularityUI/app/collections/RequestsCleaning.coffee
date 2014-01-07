Requests = require './Requests'

class RequestsCleaning extends Requests

    url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/queued/cleanup"

    parse: (requests) ->
        _.each requests, (request, i) ->
            request.JSONString = utils.stringJSON request
            request.id = request.requestId
            request.cleanupType = constants.requestCleanupTypes[request.cleanupType]
            request.timestampHuman = utils.humanTimeAgo request.timestamp
            requests[i] = request
            app.allRequests[request.id] = request

        requests

module.exports = RequestsCleaning