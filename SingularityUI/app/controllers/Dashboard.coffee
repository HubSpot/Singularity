Controller = require './Controller'

Requests = require '../collections/Requests'
RequestsStarred = require '../collections/RequestsStarred'

DashboardView = require '../views/dashboard'

class DashboardController extends Controller

    initialize: ->
        @collections.starredRequests = new RequestsStarred
        @collections.requests = new Requests [], state: 'all'

        @view = new DashboardView
            collections: @collections
            controller:  @

    refresh: ->
        @collections.starredRequests.fetch()
        @collections.requests.fetch()

module.exports = DashboardController
