Requests = require './Requests'

class RequestsPending extends Requests

    url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/queued/pending"

    parse: (requests) ->
        _.each requests, (request, i) =>
            request.id = request.id
            request.pendingType = request.pendingType
            request.JSONString = utils.stringJSON request
            app.allRequests[request.id] = request

        requests

module.exports = RequestsPending