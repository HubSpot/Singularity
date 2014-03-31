Requests = require './Requests'
Request = require '../models/Request'

class RequestsActive extends Requests

    model: Request

    url: ->
        properties = [
            'id'
            'owners'
            'numRetriesOnFailure'
            'maxFailuresBeforePausing'
            'schedule'
            'daemon'
            'instances'
            'rackSensitive'
            'deployState'
            'activeDeploy'
            'pendingDeploy'
        ]

        propertiesString = "?property=#{ properties.join('&property=') }"

        "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/active#{ propertiesString }"

    parse: (requests) ->
        _.each requests, (request, i) =>
            request.JSONString = utils.stringJSON request
            request.id = request.id
            request.name = request.name ? request.id
            request.daemon = if _.isNull(request.daemon) then true else request.daemon
            request.deployUser = (request.deployState?.activeDeploy?.user ? '').split('@')[0]
            request.deployId = request.deployState?.activeDeploy?.deployId
            request.timestamp = request.deployState?.activeDeploy?.timestamp
            request.timestampHuman = utils.humanTimeAgo request.timestamp
            request.scheduled = utils.isScheduledRequest request
            request.onDemand = utils.isOnDemandRequest request
            requests[i] = request
            app.allRequests[request.id] = request

        requests

    comparator: 'timestamp'

module.exports = RequestsActive