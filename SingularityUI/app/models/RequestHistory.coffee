Model = require './model'

localRequestHistoryIdNumber = 1

class RequestHistory extends Model

    url: -> "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/request/#{ @requestId }/requests"

    initialize: (models, { @requestId }) =>

    parse: (requestHistoryObjects) ->
        requestHistory = {}
        requestHistory.requestId = @requestId
        requestHistory.requestUpdates = requestHistoryObjects

        _.each requestHistory.requestUpdates, (requestUpdate, i) =>
            if requestUpdate.request?
                requestUpdate.request.JSONString = utils.stringJSON requestUpdate.request
                requestUpdate.request.daemon = if _.isNull(requestUpdate.request.daemon) then true else requestUpdate.request.daemon
                requestUpdate.request.localRequestHistoryId = "__localRequestHistoryId-#{ localRequestHistoryIdNumber }"
                localRequestHistoryIdNumber += 1
                app.allRequestHistories[requestUpdate.request.localRequestHistoryId] = requestUpdate.request

            requestUpdate.userHuman = requestUpdate.user
            requestUpdate.createdAtHuman = utils.humanTimeAgo requestUpdate.createdAt
            requestUpdate.stateHuman = constants.requestStates[requestUpdate.state]

        _.sortBy requestHistory.requestUpdates, (r) -> r.createdAt
        requestHistory.requestUpdates.reverse()

        requestHistory

module.exports = RequestHistory