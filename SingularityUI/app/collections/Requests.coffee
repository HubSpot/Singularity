Collection = require './collection'

Request = require '../models/Request'

class Requests extends Collection

    model: Request

    comparator: (one, two) ->
        one.get('requestDeployState')?.activeDeploy?.timestamp - two.get('requestDeployState')?.activeDeploy?.timestamp

    # If you only want to get certain properties for a particual state, put them here
    propertyFilters:
        active: [ 'request', 'requestDeployState' ]

    initialize: (models, { @state }) ->
        @state = if not @state? or @state is 'all' then '' else @state
        @state = if @state is 'cleaning' then 'cleanup' else @state

    url: ->
        # We might need to hit the queue endpoint instead
        queueApi = if @state in ['cleanup', 'pending'] then 'queued/' else ''

        propertyString = $.param property: @propertyFilters[@state] or [], true
        "#{ config.apiRoot }/requests/#{ queueApi }#{ @state }?#{ propertyString or '' }"

    getStarredRequests: ->
        jsonRequests = localStorage.getItem 'starredRequests'
        return [] if not jsonRequests?

        JSON.parse jsonRequests

    isStarred: (id) ->
        starredRequests = @getStarredRequests()
        id in starredRequests

    getStarredOnly: ->
        starredRequests = @getStarredRequests()
        return [] if _.isEmpty starredRequests

        @filter (request) =>
            request.get('request').id in starredRequests

    toggleStar: (requestId) ->
        starredRequests = @getStarredRequests()
        if requestId in starredRequests
            starredRequests = _.without starredRequests, requestId
        else
            starredRequests.push requestId

        localStorage.setItem 'starredRequests', JSON.stringify starredRequests

module.exports = Requests