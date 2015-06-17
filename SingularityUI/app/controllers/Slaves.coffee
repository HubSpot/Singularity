Controller = require './ReactController'

SlavesView = require '../views/SlavesView'

Slave = require '../models/Slave'
Slaves = require '../collections/Slaves'

class SlavesController extends Controller

    initialize: ->
    
        app.showPageLoader()
        @slavesCollection = new Slaves []

        new SlavesView
            collection: @slavesCollection
            model: Slave

module.exports = SlavesController