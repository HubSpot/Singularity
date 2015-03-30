Controller = require './Controller'

Slaves = require '../collections/Slaves'

SlavesView = require '../views/slaves'
SimpleSubview = require '../views/simpleSubview'

class SlavesController extends Controller

    initialize: ->
        app.showPageLoader()
        @collections.slaves         = new Slaves []
        @setView new SlavesView
            collection: @collections.slaves

        app.showView @view

        @refresh()

    refresh: ->
        @collections.slaves.fetch reset: true

module.exports = SlavesController
