Controller = require './Controller'

Racks = require '../collections/Racks'

RacksView = require '../views/racks'
SimpleSubview = require '../views/simpleSubview'

class RacksController extends Controller

    templates:
        racks:         require '../templates/racks/racks'

    initialize: ->
        app.showPageLoader()

        @collections.activeRacks     = new Racks [], rackStates: ['ACTIVE']
        @collections.decomRacks      = new Racks [], rackStates: ['DECOMMISSIONING', 'STARTING_DECOMISSION', 'DECOMISSIONED']
        @collections.inactiveRacks   = new Racks [], rackStates: ['DEAD', 'MISSING_ON_STARTUP']

        # Subviews for the tables
        @subviews.activeRacks         = new SimpleSubview
            collection: @collections.activeRacks
            template:   @templates.racks
        @subviews.decomRacks          = new SimpleSubview
            collection: @collections.decomRacks
            template:   @templates.racks
        @subviews.inactiveRacks       = new SimpleSubview
            collection: @collections.inactiveRacks
            template:   @templates.racks

        @setView new RacksView
            subviews:   @subviews

        app.showView @view

        @refresh()

    refresh: ->
        @collections.activeRacks.fetch()
        @collections.decomRacks.fetch()
        @collections.inactiveRacks.fetch()

module.exports = RacksController
