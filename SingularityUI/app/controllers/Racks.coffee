Controller = require './ReactController'

RacksView = require '../views/RacksView'

Rack = require '../models/Rack'
Racks = require '../collections/Racks'

class RacksController extends Controller

    initialize: ->
    
        app.showPageLoader()
        @racksCollection = new Racks []

        new RacksView
            collection: @racksCollection
            model: Rack

module.exports = RacksController