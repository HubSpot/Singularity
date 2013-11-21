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
            racksActive: _.pluck(@racksActive.models, 'attributes')
            racksDead: _.pluck(@racksDead.models, 'attributes')
            racksDecomissioning: _.pluck(@racksDecomissioning.models, 'attributes')

        @$el.html @template context

        utils.setupSortableTables()

module.exports = RacksView