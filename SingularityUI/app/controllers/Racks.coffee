Controller = require './Controller'

Racks = require '../collections/Racks'

RacksView = require '../views/racks'
SimpleSubview = require '../views/simpleSubview'

class RacksController extends Controller

    templates:
        activeRacks:         require '../templates/racks/active'
        deadRacks:           require '../templates/racks/dead'
        decomissioningRacks: require '../templates/racks/decomissioning'

    initialize: ->
        app.showPageLoader()

        @collections.activeRacks         = new Racks [], rackType: 'active'
        @collections.deadRacks           = new Racks [], rackType: 'dead'
        @collections.decomissioningRacks = new Racks [], rackType: 'decomissioning'

        # Subviews for the tables
        @subviews.activeRacks         = new SimpleSubview
            collection: @collections.activeRacks
            template:   @templates.activeRacks

        @subviews.deadRacks           = new SimpleSubview
            collection: @collections.deadRacks
            template:   @templates.deadRacks

        @subviews.decomissioningRacks = new SimpleSubview
            collection: @collections.decomissioningRacks
            template:   @templates.decomissioningRacks

        @view = new RacksView
            subviews:   @subviews
            controller: @

        app.showView @view

        @refresh()

    refresh: ->
        @collections.activeRacks.fetch()
        @collections.deadRacks.fetch()
        @collections.decomissioningRacks.fetch()

module.exports = RacksController
