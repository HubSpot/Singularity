Controller = require './Controller'

Racks = require '../collections/Racks'

RacksView = require '../views/racks'
SimpleSubview = require '../views/simpleSubview'

class RacksController extends Controller

    initialize: ({@state}) ->
        app.showPageLoader()
        @collections.racks = new Racks []
        @setView new RacksView _.extend {@state},
            collection: @collections.racks

        app.showView @view

        @refresh()

    refresh: ->
        @collections.racks.fetch()

module.exports = RacksController