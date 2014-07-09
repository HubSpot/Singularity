View = require './view'

Requests = require '../collections/Requests'
RequestsStarred = require '../collections/RequestsStarred'

class DashboardView extends View

    template: require './templates/dashboard'

    events: ->
        _.extend super,
            'click [data-action="unstar"]': 'unstar'
            'click [data-action="change-user"]': 'changeUser'

    initialize: =>
        app.user.on 'change', @render, @

        @starredCollection = new RequestsStarred
        @starredCollection.fetch()

        @collection = new Requests state: 'all'
        @collection.fetch().done @render

    render: =>
        deployUser = app.user.get('deployUser')

        # Filter starred requests
        starredRequests = @collection.filter (request) =>
            @starredCollection.get(request.get 'id')?

        starredRequests = _.pluck starredRequests, 'attributes'

        # Count up the Requests for the clicky boxes
        userRequests = @collection.where {deployUser}
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
            collectionSynced: @collection.synced
            userRequestTotals: userRequestTotals or { }
            starredRequests: starredRequests or []

        @$el.html @template context

        utils.setupSortableTables()

    unstar: (e) =>
        $target = $(e.target)
        $table = $target.parents('table')

        requestName = $target.data('request-name')

        app.collections.requestsStarred.toggle(requestName)

        $table.find("""[data-request-name="#{ requestName }"]""").each -> $(@).parents('tr').remove()

        if $table.find('tbody tr').length is 0
            @render()

    changeUser: =>
        app.deployUserPrompt()

module.exports = DashboardView