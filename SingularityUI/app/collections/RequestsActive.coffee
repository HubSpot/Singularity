Requests = require './Requests'

class RequestsActive extends Requests

    url: "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/requests"

    parse: (requests) ->
        _.each requests, (request, i) =>
            request.JSONString = utils.stringJSON request
            request.id = request.id
            request.name = request.name ? request.id
            request.deployUser = (request.executorData?.env?.DEPLOY_USER ? '').split('@')[0]
            request.timestampHuman = if request?.timestamp? then moment(request.timestamp).from() else ''
            requests[i] = request
            app.allRequests[request.id] = request

        requests

module.exports = RequestsActive