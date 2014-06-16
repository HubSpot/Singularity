Requests = require './Requests'
Request = require '../models/Request'

class RequestsActive extends Requests

    model: Request

    url: ->
        properties = [
            'request'
            'requestDeployState'
        ]

        propertiesString = "?property=#{ properties.join('&property=') }"

        "#{ config.apiRoot }/requests/active#{ propertiesString }"

    parse: (requests) ->
        _.each requests, (request, i) =>
            request.JSONString = utils.stringJSON request
            request.id = request.request.id
            request.request.instances = if _.isNull(request.request.instances) then 1 else request.request.instances
            request.instances = request.request.instances
            request.schedule = request.request.schedule
            request.name = request.name ? request.id
            request.daemon = request.request.daemon
            request.daemon = if _.isNull(request.daemon) then true else request.daemon
            request.deployUser = (request.requestDeployState?.activeDeploy?.user ? '').split('@')[0]
            request.deployId = request.requestDeployState?.activeDeploy?.deployId
            request.timestamp = request.requestDeployState?.activeDeploy?.timestamp
            request.timestampHuman = utils.humanTimeAgo request.timestamp
            request.timestampHumanShort = utils.humanTimeShort request.timestamp
            requests[i] = request
            app.allRequests[request.id] = request

        requests

    comparator: 'timestamp'

module.exports = RequestsActive