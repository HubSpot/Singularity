Controller = require './ReactController'

AdminSubview = require '../views/AdminSubview'

Slave = require '../models/Slave'
Slaves = require '../collections/Slaves'

class SlavesController extends Controller

    initialize: ->
    
        app.showPageLoader()
        @slavesCollection = new Slaves []

        new AdminSubview
            collection: @slavesCollection
            model: Slave
            label: 'slaves'

module.exports = SlavesController