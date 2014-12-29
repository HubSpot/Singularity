Controller = require './Controller'

Slaves = require '../collections/Slaves'

SlavesView = require '../views/slaves'
SimpleSubview = require '../views/simpleSubview'

class SlavesController extends Controller

    templates:
        slaves:         require '../templates/slaves/slaves'

    initialize: ->
        app.showPageLoader()

        @collections.slaves         = new Slaves []

        # Subviews for the tables
        @subviews.slaves         = new SimpleSubview
            collection: @collections.slaves
            template:   @templates.slaves

        @setView new SlavesView
            subviews: @subviews

        app.showView @view

        @refresh()

    refresh: ->
        @collections.slaves.fetch()

module.exports = SlavesController
