Collection = require './collection'

class RequestsPending extends Collection

    url: "#{ config.apiRoot }/requests/queued/pending"

    parse: (requests) ->
        for request in requests
            request.displayState = constants.requestStates.PENDING
            request.id = request.requestId
            request.JSONString = utils.stringJSON request
            request.timestampHuman = utils.humanTimeAgo request.timestamp

        requests

module.exports = RequestsPending