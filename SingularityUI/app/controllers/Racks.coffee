Controller = require './Controller'

Racks = require '../collections/Racks'

RacksView = require '../views/racks'
SimpleSubview = require '../views/simpleSubview'

class RacksController extends Controller

    templates:
        racks:         require '../templates/racks/racks'

    initialize: ->
        app.showPageLoader()

        @collections.racks     = new Racks []

        # Subviews for the tables
        @subviews.racks         = new SimpleSubview
            collection: @collections.racks
            template:   @templates.racks

        @setView new RacksView
            subviews:   @subviews

        app.showView @view

        @refresh()

    refresh: ->
        @collections.racks.fetch()

module.exports = RacksController
