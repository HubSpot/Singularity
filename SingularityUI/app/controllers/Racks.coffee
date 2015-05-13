Controller = require './ReactController'

AdminSubview = require '../views/AdminSubview'

Racks = require '../collections/Racks'
Rack = require '../models/Rack'

class RacksController extends Controller

    initialize: ->
    
        app.showPageLoader()
        @racksCollection = new Racks []

        new AdminSubview
            collection: @racksCollection
            model: Rack
            label: 'racks'


module.exports = RacksController