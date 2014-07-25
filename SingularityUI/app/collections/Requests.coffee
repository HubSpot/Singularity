Collection = require './collection'

Request = require '../models/Request'

class Requests extends Collection

    model: Request

    comparator: 'timestamp'

    # If you only want to get certain properties for a particual state, put them here
    propertyFilters:
        active: [ 'request', 'requestDeployState' ]

    initialize: (models, { @state }) ->
        @state = if not @state? or @state is 'all' then '' else @state

    url: ->
        propertyString = $.param property: @propertyFilters[@state] or [], true
        "#{ config.apiRoot }/requests/#{ @state }?#{ propertyString or '' }"

    parse: (requests) ->
        for request in requests
            request.originalObject = _.clone request
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

        requests

module.exports = Requests