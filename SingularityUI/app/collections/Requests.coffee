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

    getUserRequestsTotals: ->
        deployUser = app.user.get 'deployUser'
        userRequests = @.filter (model) ->
            request = model.get('request')
            deployUserTrimmed = deployUser.split("@")[0]
            return false if not request.owners
            for owner in request.owners
                ownerTrimmed = owner.split("@")[0]
                return true if deployUserTrimmed == ownerTrimmed
            return false

        userRequestTotals =
            all: userRequests.length
            onDemand: 0
            worker: 0
            scheduled: 0
            runOnce: 0
            service: 0

        for request in userRequests
            if request.type == 'ON_DEMAND'  then userRequestTotals.onDemand  += 1
            if request.type == 'SCHEDULED'  then userRequestTotals.scheduled += 1
            if request.type == 'WORKER'     then userRequestTotals.worker    += 1
            if request.type == 'RUN_ONCE'   then userRequestTotals.runOnce   += 1
            if request.type == 'SERVICE'    then userRequestTotals.service   += 1

        data = [
            { linkName: "all",        label: 'total',     total: userRequestTotals.all }
            { linkName: "ON_DEMAND",  label: 'On Demand', total: userRequestTotals.onDemand }    
            { linkName: "SCHEDULED",  label: 'Scheduled', total: userRequestTotals.scheduled }
            { linkName: "WORKER",     label: 'Worker',    total: userRequestTotals.worker }
            { linkName: "RUN_ONCE",   label: 'Run Once',  total: userRequestTotals.runOnce }
            { linkName: "SERVICE",    label: 'Service',   total: userRequestTotals.service }
        ]

        for request in data
            request.link = "#{config.appRoot}/requests/active/#{name}/all/#{deployUser}"

        return data

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


    sortCollection: (newSortAttribute) ->
        @isSorted = true

        if newSortAttribute is @sortAttribute and @sortAscending?
            @sortAscending = not @sortAscending
        else
            # timestamp should be DESC by default
            @sortAscending = if newSortAttribute is "timestamp" then false else true
        
        @sortAttribute = newSortAttribute
        requests = _.pluck @getStarredOnly(), "attributes"
        
        # Sort the table if the user clicked on the table heading things
        if @sortAttribute?
            requests = _.sortBy requests, (request) =>

                # Traverse through the properties to find what we're after
                attributes = @sortAttribute.split '.'
                value = request
                for attribute in attributes
                    value = value[attribute]                
                    value = '' if not value?
                return value

            if not @sortAscending
                requests = requests.reverse()
        else
            requests.reverse()

        return requests
    
       

module.exports = Requests