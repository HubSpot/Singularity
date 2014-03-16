Requests = require './Requests'

class RequestsPending extends Requests

    url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/queued/pending"

    parse: (requests) ->
        _.each requests, (request, i) =>
            request.id = request.requestId
            request.JSONString = utils.stringJSON request
            request.timestampHuman = utils.humanTimeAgo request.timestamp
            app.allRequests[request.id] = request

        requests

module.exports = RequestsPending