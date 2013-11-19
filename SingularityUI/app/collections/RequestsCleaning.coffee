Requests = require './Requests'

class RequestsCleaning extends Requests

    url: "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/requests/queued/cleanup"

    parse: (requests) ->
        _.each requests, (requestIdString, i) ->
            request = {}
            request.JSONString = utils.stringJSON request
            request.id = requestIdString
            requests[i] = request
            app.allRequests[request.id] = request

        requests

module.exports = RequestsCleaning