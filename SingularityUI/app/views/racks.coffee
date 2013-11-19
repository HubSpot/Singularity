View = require './view'

Racks = require '../collections/Racks'

class RacksView extends View

    template: require './templates/racks'

    initialize: =>
        promises = []

        @racksActive = new Racks [], rackType: 'active'
        promises.push @racksActive.fetch()

        @racksDead = new Racks [], rackType: 'dead'
        promises.push @racksDead.fetch()

        @racksDecomissioning = new Racks [], rackType: 'decomissioning'
        promises.push @racksDecomissioning.fetch()

        $.when(promises...).done =>
            @fetchDone = true
            @render()

    render: =>
        return unless @fetchDone

        context =
            racksActive: @racksActive.toJSON()
            racksDead: @racksDead.toJSON()
            racksDecomissioning: @racksDecomissioning.toJSON()

        @$el.html @template context

        utils.setupSortableTables()

module.exports = RacksView