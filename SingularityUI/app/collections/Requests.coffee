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
        @state = if not @state? or @state in ['all', 'activeDeploy', 'noDeploy'] then '' else @state
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

    getUserRequests: (user) ->
        @filter (model) ->
            request = model.get('request')

            deployUserTrimmed = user.split("@")[0]

            activeDeployUser = model.get('requestDeployState')?.activeDeploy?.user

            if activeDeployUser
                activeDeployUserTrimmed = activeDeployUser.split('@')[0]
                if deployUserTrimmed is activeDeployUserTrimmed
                    return true

            if not request.owners
                return false

            for owner in request.owners
                ownerTrimmed = owner.split("@")[0]
                if deployUserTrimmed is ownerTrimmed
                    return true
            return false

    # Get `active` request type totals by user
    getUserRequestTotals: (user) ->
        userRequests = @getUserRequests user

        userRequestTotals =
            all: userRequests.length
            onDemand: 0
            worker: 0
            scheduled: 0
            runOnce: 0
            service: 0

        for request in userRequests

            type = request.get 'type'

            continue if request.get('state') isnt 'ACTIVE'

            if type is 'ON_DEMAND'  then userRequestTotals.onDemand  += 1
            if type is 'SCHEDULED'  then userRequestTotals.scheduled += 1
            if type is 'WORKER'     then userRequestTotals.worker    += 1
            if type is 'RUN_ONCE'   then userRequestTotals.runOnce   += 1
            if type is 'SERVICE'    then userRequestTotals.service   += 1

        userRequestTotals

module.exports = Requests
