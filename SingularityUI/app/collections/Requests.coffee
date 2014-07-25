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

    getStarredRequests: ->
        jsonRequests = localStorage.getItem 'starredRequests'
        return [] if not jsonRequests?

        JSON.parse jsonRequests

    getStarredOnly: ->
        starredRequests = @getStarredRequests()
        return [] if _.isEmpty starredRequests

        @filter (request) =>
            request.get('request').id in starredRequests

    toggleStar: (requestId) ->
        starredRequests = @getStarredRequests
        if requestId in starredRequests
            starredRequests = _.without starredRequests, requestId
        else
            starredRequests.push requestId

        localStorage.setItem 'starredRequests', JSON.stringify starredRequests

    parse: (requests) ->
        for request in requests
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

        requests

module.exports = Requests