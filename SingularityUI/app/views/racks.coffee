View = require './view'

Racks = require '../collections/Racks'

class RacksView extends View

    template: require './templates/racks'

    initialize: =>
        @racksActive = new Racks [], rackType: 'active'
        @racksActive.fetch().done => @render()

        @racksDead = new Racks [], rackType: 'dead'
        @racksDead.fetch().done => @render()

        @racksDecomissioning = new Racks [], rackType: 'decomissioning'
        @racksDecomissioning.fetch().done => @render()

    render: =>
        context =
            racksActive: @racksActive.toJSON()
            racksDead: @racksDead.toJSON()
            racksDecomissioning: @racksDecomissioning.toJSON()

        @$el.html @template context

module.exports = RacksView