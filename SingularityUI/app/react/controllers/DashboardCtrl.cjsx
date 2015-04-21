Controller = require './Controller'
DashboardView = require '../views/DashboardView'
Requests = require '../../collections/Requests'

class ReactDashboardController extends Controller

    initialize: ->
        app.showPageLoader()

        @requestsCollection = new Requests [], state: 'all'

        new DashboardView
            collection: @requestsCollection

module.exports = ReactDashboardController