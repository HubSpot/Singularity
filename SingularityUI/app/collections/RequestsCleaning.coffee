Requests = require './Requests'

class RequestsCleaning extends Requests

    url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/queued/cleanup"

    parse: (requests) ->
        _.each requests, (request, i) ->
            request.JSONString = utils.stringJSON request
            request.id = request.requestId
            request.cleanupType = constants.requestCleanupType[request.cleanupType]
            request.timestampHuman = if request.timestamp? then moment(request.timestamp).from() else ''
            requests[i] = request
            app.allRequests[request.id] = request

        requests

module.exports = RequestsCleaning