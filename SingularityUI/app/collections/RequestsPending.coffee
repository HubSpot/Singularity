Collection = require './collection'

class RequestsPending extends Collection

    url: "#{ config.apiRoot }/requests/queued/pending"

    parse: (requests) ->
        _.each requests, (request, i) =>
            request.displayState = constants.requestStates.PENDING
            request.id = request.requestId
            request.JSONString = utils.stringJSON request
            request.timestampHuman = utils.humanTimeAgo request.timestamp
            app.allRequests[request.id] = request

        requests

module.exports = RequestsPending