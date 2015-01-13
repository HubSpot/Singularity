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
