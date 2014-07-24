View = require './view'

class DashboardView extends View

    template: require '../templates/dashboard'

    events: ->
        _.extend super,
            'click [data-action="unstar"]': 'unstar'
            'click [data-action="change-user"]': 'changeUser'

    initialize: =>
        @listenTo app.user, 'change', @render
        @listenTo @collections.requests, 'sync', @render

    render: =>
        deployUser = app.user.get 'deployUser'

        # Filter starred requests
        starredRequests = @collections.requests.filter (request) =>
            @collections.starredRequests.get(request.get 'id')?

        starredRequests = _.pluck starredRequests, 'attributes'

        # Count up the Requests for the clicky boxes
        userRequests = @collections.requests.where {deployUser}
        userRequestTotals =
            all: userRequests.length
            daemon:    0
            onDemand:  0
            scheduled: 0

        _.each userRequests, (request) =>
            if request.get('onDemand')  then userRequestTotals.onDemand  += 1
            if request.get('scheduled') then userRequestTotals.scheduled += 1
            if request.get('daemon')    then userRequestTotals.daemon    += 1

        context =
            deployUser: deployUser
            collectionSynced: @collections.requests.synced
            userRequestTotals: userRequestTotals or { }
            starredRequests: starredRequests or []

        @$el.html @template context

    unstar: (e) =>
        $target = $ e.currentTarget
        $row = $target.parents 'tr'

        id = $row.data 'request-id'

        @collections.starredRequests.toggle id

        $row.remove()

        if @$('tbody tr').length is 0
            @render()

    changeUser: =>
        app.deployUserPrompt()

module.exports = DashboardView
