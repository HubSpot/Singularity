View = require './view'

class DashboardView extends View

    template: require '../templates/dashboard'

    events: ->
        _.extend super,
            'click [data-action="unstar"]': 'unstar'
            'click [data-action="change-user"]': 'changeUser'

    initialize: =>
        @listenTo app.user, 'change', @render
        @listenTo @collection, 'sync', @render

    render: =>
        deployUser = app.user.get 'deployUser'

        # Filter starred requests
        starredRequests = @collection.getStarredOnly()
        starredRequests = _.pluck starredRequests, 'attributes'

        # Count up the Requests for the clicky boxes
        userRequests = @collection.filter (model) ->
          request = model.get('request')
          deployUserTrimmed = deployUser.split("@")[0]
          
          if not request.owners
            return false
            
          for owner in request.owners
            ownerTrimmed = owner.split("@")[0]
            if deployUserTrimmed == ownerTrimmed
              return true
          
          return false
        userRequestTotals =
            all: userRequests.length
            onDemand:    0
            worker:  0
            scheduled: 0
            runOnce: 0
            service: 0

        _.each userRequests, (request) =>
            if request.type == 'ON_DEMAND'  then userRequestTotals.onDemand  += 1
            if request.type == 'SCHEDULED'  then userRequestTotals.scheduled += 1
            if request.type == 'WORKER'     then userRequestTotals.worker    += 1
            if request.type == 'RUN_ONCE'   then userRequestTotals.runOnce   += 1
            if request.type == 'SERVICE'    then userRequestTotals.service   += 1

        context =
            deployUser: deployUser
            collectionSynced: @collection.synced
            userRequestTotals: userRequestTotals or { }
            starredRequests: starredRequests or []

        @$el.html @template context

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
