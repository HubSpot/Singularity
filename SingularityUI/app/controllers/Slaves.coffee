Controller = require './Controller'

Slaves = require '../collections/Slaves'

SlavesView = require '../views/slaves'
SimpleSubview = require '../views/simpleSubview'

class SlavesController extends Controller

    templates:
        slaves:         require '../templates/slaves/slaves'

    initialize: ->
        app.showPageLoader()

        @collections.activeSlaves         = new Slaves [], slaveStates: ['ACTIVE']
        @collections.decomSlaves          = new Slaves [], slaveStates: ['DECOMMISSIONING', 'STARTING_DECOMISSION', 'DECOMISSIONED']
        @collections.inactiveSlaves       = new Slaves [], slaveStates: ['DEAD', 'MISSING_ON_STARTUP']

        # Subviews for the tables
        @subviews.activeSlaves         = new SimpleSubview
            collection: @collections.activeSlaves
            template:   @templates.slaves
        @subviews.decomSlaves         = new SimpleSubview
            collection: @collections.decomSlaves
            template:   @templates.slaves
        @subviews.inactiveSlaves         = new SimpleSubview
            collection: @collections.inactiveSlaves
            template:   @templates.slaves

        @setView new SlavesView
            subviews: @subviews

        app.showView @view

        @refresh()

    refresh: ->
        @collections.activeSlaves.fetch()
        @collections.decomSlaves.fetch()
        @collections.inactiveSlaves.fetch()

module.exports = SlavesController
