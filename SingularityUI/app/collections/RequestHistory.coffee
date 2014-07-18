PaginableCollection = require './PaginableCollection'

class RequestHistory extends PaginableCollection

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/requests"

    initialize: (models, { @requestId }) =>

    parse: (requestHistoryObjects) ->
        _.each requestHistoryObjects, (requestUpdate, i) =>
            if requestUpdate.request?
                requestUpdate.request.JSONString = utils.stringJSON requestUpdate.request
                requestUpdate.request.daemon = if _.isNull(requestUpdate.request.daemon) then true else requestUpdate.request.daemon
                app.allRequestHistories[requestUpdate.request.localRequestHistoryId] = requestUpdate.request

            requestUpdate.userHuman = requestUpdate.user
            requestUpdate.createdAtHuman = utils.humanTimeAgo requestUpdate.createdAt
            requestUpdate.stateHuman = constants.requestHistoryStates[requestUpdate.state]

        requestHistoryObjects

module.exports = RequestHistory
