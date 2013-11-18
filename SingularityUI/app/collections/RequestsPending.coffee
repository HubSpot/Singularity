Collection = require './collection'

Requests = require './Requests'

class RequestsPending extends Requests

    url: "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/requests/queued/pending"

    parse: (requests) ->
        _.each requests, (request, i) =>
            request.id = request.id
            request.pendingType = request.pendingType

        requests

module.exports = RequestsPending