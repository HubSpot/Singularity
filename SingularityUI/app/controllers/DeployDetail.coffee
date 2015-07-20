Controller = require './Controller'

RequestDeployStatus                 = require '../models/RequestDeployStatus'
#Task                   = require '../collections/Task'

DeployDetailView      = require '../views/deploy'
ExpandableTableSubview = require '../views/expandableTableSubview'
SimpleSubview          = require '../views/simpleSubview'

class DeployDetailController extends Controller

    templates:
        header:             require '../templates/deployDetail/deployHeader'

        # Subview templates

    initialize: ({@requestId, @deployId}) ->
        #
        # Data stuff
        #
        @models.deploy = new RequestDeployStatus
            deployId: @deployId
            requestId: @requestId

        #
        # Subviews
        #
        @subviews.header = new SimpleSubview
            model:      @models.deploy
            template:   @templates.header


        #
        # Main view & stuff
        #
        @setView new DeployDetailView _.extend {@requestId, @deployId, @subviews},
            model: @models.deploy

        @refresh()

        app.showView @view

    refresh: ->
        requestFetch = @models.deploy.fetch()

        requestFetch.error =>
            # ignore 404 so we can still display info about
            # deleted requests (show in `requestHistoryMsg`)
            @ignore404
            app.caughtError()

module.exports = DeployDetailController
