Controller = require './Controller'

Requests = require '../collections/Requests'

DashboardView = require '../views/dashboard'

class DashboardController extends Controller

    initialize: ->
        app.showPageLoader()

        @collections.requests = new Requests [], state: 'all'

        @collections.requests.fetch().done =>
            @view = new DashboardView
                collection: @collections.requests

            app.showView @view

    refresh: ->
        @collections.requests.fetch()

module.exports = DashboardController
