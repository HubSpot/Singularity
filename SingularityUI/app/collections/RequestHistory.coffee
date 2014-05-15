Collection = require './collection'

localRequestHistoryIdNumber = 1

class RequestHistory extends Mixen(Teeble.ServerCollection)

    url: ->
        params =
            count: @perPage
            page: @currentPage
            orderDirection: 'DESC'

        "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/request/#{ @requestId }/requests?#{ $.param params }"

    initialize: (models, { @requestId }) =>
        super

    parse: (requestHistoryObjects) ->
        requestHistoryObjects

        _.each requestHistoryObjects, (requestUpdate, i) =>
            if requestUpdate.request?
                requestUpdate.request.JSONString = utils.stringJSON requestUpdate.request
                requestUpdate.request.daemon = if _.isNull(requestUpdate.request.daemon) then true else requestUpdate.request.daemon
                requestUpdate.request.localRequestHistoryId = "__localRequestHistoryId-#{ localRequestHistoryIdNumber }"
                localRequestHistoryIdNumber += 1
                app.allRequestHistories[requestUpdate.request.localRequestHistoryId] = requestUpdate.request

            requestUpdate.userHuman = requestUpdate.user
            requestUpdate.createdAtHuman = utils.humanTimeAgo requestUpdate.createdAt
            requestUpdate.stateHuman = constants.requestStates[requestUpdate.state]

        requestHistoryObjects

module.exports = RequestHistory
