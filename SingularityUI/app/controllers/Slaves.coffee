Controller = require './Controller'

Slaves = require '../collections/Slaves'

SlavesView = require('../views/slaves').default
SimpleSubview = require '../views/simpleSubview'

class SlavesController extends Controller

    initialize: ({@state}) ->
        app.showPageLoader()
        @title 'Slaves'

        @collections.slaves         = new Slaves []
        @setView new SlavesView _.extend {@state},
            collection: @collections.slaves

        app.showView @view

        @refresh()

    refresh: ->
        @collections.slaves.fetch().done => @view.render()

module.exports = SlavesController
