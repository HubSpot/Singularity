Controller = require './Controller'

Racks = require '../collections/Racks'

RacksView = require('../views/racks').default
SimpleSubview = require '../views/simpleSubview'

class RacksController extends Controller

    initialize: ({@state}) ->
        app.showPageLoader()
        @title 'Racks'
        @collections.racks = new Racks []
        @setView new RacksView _.extend {@state},
            collection: @collections.racks

        app.showView @view

        @refresh()

    refresh: ->
        @collections.racks.fetch().done =>
            @view.render()

module.exports = RacksController
