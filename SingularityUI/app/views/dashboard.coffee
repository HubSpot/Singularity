View = require './view'

class DashboardView extends View

    templateBase: require '../templates/dashboard'
    templateRequestsTable: require '../templates/dashboardTable/dashboardStarred'
    templateRequestsPausedBody: require '../templates/requestsTable/requestsPausedBody'

    events: ->
        _.extend super,
            'click [data-action="unstar"]': 'unstar'
            'click [data-action="change-user"]': 'changeUser'
            'click th[data-sort-attribute]': 'sortTable'
            'click [data-action="viewJSON"]': 'viewJson'
            'click [data-action="remove"]': 'removeRequest'
            'click [data-action="unpause"]': 'unpauseRequest'

    initialize: =>
        @listenTo app.user, 'change', @render
        @listenTo @collection, 'sync', @render

    render: =>
        deployUser = app.getUsername()

        partials =
            partials:
                requestsBody: @templateRequestsTable
                requestsPausedBody: @templateRequestsPausedBody

        # Count up the Requests for the clicky boxes
        userRequestTotals = @collection.getUserRequestTotals deployUser

        pausedRequests = _.map(_.filter(@collection.getUserRequests(app.getUsername()), (r) -> r.get('state') is 'PAUSED'), (r) -> r.toJSON())

        context =
            deployUser: deployUser
            collectionSynced: @collection.synced
            userRequestTotals: userRequestTotals or { }
            haveStarredRequests: @collection.getStarredOnly().length
            pausedRequests:
                requests: pausedRequests
                haveRequests: pausedRequests.length > 0
                requestsSubFilter: ''
                hideNewRequestButton: config.hideNewRequestButton
                collectionSynced: @collection.synced

        @$el.html @templateBase context, partials
        @renderTable()

        super.afterRender()

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

    getRequest: (id) =>
        maybeRequest = @collection.models.filter (model) ->
            model.id is id
        if maybeRequest
            return maybeRequest[0]
        else
            return

    viewJson: (e) ->
        id = $(e.target).parents('tr').data 'request-id'
        request = @getRequest id
        unless request
            Messenger().error
                message: "<p>Could not find request #{id}. Perhaps someone removed it?</p>"
            return
        utils.viewJSON request

    removeRequest: (e) ->
        $row = $(e.target).parents 'tr'
        id = $row.data('request-id')
        request = @getRequest id
        unless request
            Messenger().error
                message: "<p>Could not find request #{id}. Perhaps someone removed it first?</p>"
            return
        request.promptRemove =>
            $row.remove()

    unpauseRequest: (e) ->
        $row = $(e.target).parents 'tr'
        id = $row.data('request-id')

        request = @getRequest id
        unless request
            Messenger().error
                message: "<p>Could not find request #{id}. Perhaps someone removed it?</p>"
            return

        request.promptUnpause =>
            $row.remove()
            @trigger 'refreshrequest'

module.exports = DashboardView
