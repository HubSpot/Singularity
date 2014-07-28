Controller = require './Controller'

Slaves = require '../collections/Slaves'

SlavesView = require '../views/slaves'
SimpleSubview = require '../views/simpleSubview'

class SlavesController extends Controller

    templates:
        activeSlaves:         require '../templates/slaves/active'
        deadSlaves:           require '../templates/slaves/dead'
        decomissioningSlaves: require '../templates/slaves/decomissioning'

    initialize: ->
        app.showPageLoader()

        @collections.activeSlaves         = new Slaves [], slaveType: 'active'
        @collections.deadSlaves           = new Slaves [], slaveType: 'dead'
        @collections.decomissioningSlaves = new Slaves [], slaveType: 'decomissioning'

        # Subviews for the tables
        @subviews.activeSlaves         = new SimpleSubview
            collection: @collections.activeSlaves
            template:   @templates.activeSlaves

        @subviews.deadSlaves           = new SimpleSubview
            collection: @collections.deadSlaves
            template:   @templates.deadSlaves

        @subviews.decomissioningSlaves = new SimpleSubview
            collection: @collections.decomissioningSlaves
            template:   @templates.decomissioningSlaves

        @view = new SlavesView
            subviews:   @subviews
            controller: @

        app.showView @view

        @refresh()

    refresh: ->
        @collections.activeSlaves.fetch()
        @collections.deadSlaves.fetch()
        @collections.decomissioningSlaves.fetch()

module.exports = SlavesController
