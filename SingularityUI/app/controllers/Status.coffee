Controller = require './Controller'

State = require '../models/State'

StatusView = require '../views/status'

class StatusController extends Controller
    
    initialize: ->
        app.showPageLoader()

        @models.state = new State

        @models.state.fetch().done =>
            @setView new StatusView
                model: @models.state

            app.showView @view

    refresh: ->
        @models.state.fetch()


module.exports = StatusController
