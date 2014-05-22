
localRequestHistoryIdNumber = 1

# Can't just extend Teeble.ServerCollection directly due to Mixen bugs :(
class RequestHistory extends Mixen(Teeble.ServerCollection)

    url: ->
        params =
            count: @perPage
            page: @currentPage
            orderDirection: 'DESC'

        "#{ window.singularity.config.apiBase }/history/request/#{ @requestId }/requests?#{ $.param params }"

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
