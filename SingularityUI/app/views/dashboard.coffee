View = require './view'

class DashboardView extends View

    templateBase: require '../templates/dashboard'
    templateRequestsTable: require '../templates/dashboardTable/dashboardStarred'

    events: ->
        _.extend super,
            'click [data-action="unstar"]': 'unstar'
            'click [data-action="change-user"]': 'changeUser'
            'click th[data-sort-attribute]': 'sortTable'

    initialize: =>
        @listenTo app.user, 'change', @render
        @listenTo @collection, 'sync', @render

    render: =>
        deployUser = app.getUsername()

        partials =
            partials:
                requestsBody: @templateRequestsTable

        # Count up the Requests for the clicky boxes
        if deployUser
            deployUserTrimmed = deployUser.split("@")[0]
            userRequests = @collection.filter (model) ->
                request = model.get 'request'

                return false unless request.owners

                for owner in request.owners
                    ownerTrimmed = owner.split("@")[0]
                    return true if deployUserTrimmed is ownerTrimmed

                return false

            userRequestTotals =
                all: userRequests.length
                onDemand: 0
                worker: 0
                scheduled: 0
                runOnce: 0
                service: 0

            _.each userRequests, (request) =>
                if request.type is 'ON_DEMAND'  then userRequestTotals.onDemand  += 1
                if request.type is 'SCHEDULED'  then userRequestTotals.scheduled += 1
                if request.type is 'WORKER'     then userRequestTotals.worker    += 1
                if request.type is 'RUN_ONCE'   then userRequestTotals.runOnce   += 1
                if request.type is 'SERVICE'    then userRequestTotals.service   += 1

        context =
            deployUser: deployUser
            collectionSynced: @collection.synced
            userRequestTotals: userRequestTotals or { }
            haveStarredRequests: @collection.getStarredOnly().length

        @$el.html @templateBase context, partials
        @renderTable()

    renderTable: =>
        @sortCollection()
        requests = @currentRequests

        $contents = @templateRequestsTable
            starredRequests: requests
            requests: requests

        $table = @$ ".table-staged table"
        $tableBody = $table.find "tbody"
        $tableBody.html $contents

    sortCollection: =>
        requests = _.pluck @collection.getStarredOnly(), "attributes"

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

        @currentRequests = requests

    sortTable: (event) =>
        @isSorted = true

        $target = $ event.currentTarget
        newSortAttribute = $target.attr "data-sort-attribute"

        $currentlySortedHeading = @$ "[data-sorted=true]"
        $currentlySortedHeading.removeAttr "data-sorted"
        $currentlySortedHeading.find('span').remove()


        if newSortAttribute is @sortAttribute and @sortAscending?
            @sortAscending = not @sortAscending
        else
            # timestamp should be DESC by default
            @sortAscending = if newSortAttribute is "timestamp" then false else true

        @sortAttribute = newSortAttribute

        $target.attr "data-sorted", "true"
        $target.append "<span class='glyphicon glyphicon-chevron-#{ if @sortAscending then 'up' else 'down' }'></span>"

        @renderTable()

    unstar: (e) =>
        $target = $ e.currentTarget
        $row = $target.parents 'tr'

        id = $row.data 'request-id'

        @collection.toggleStar id

        $row.remove()

        if @$('tbody tr').length is 0
            @render()

    changeUser: =>
        app.deployUserPrompt()

module.exports = DashboardView
