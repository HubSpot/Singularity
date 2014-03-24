View = require './view'

Racks = require '../collections/Racks'

class RacksView extends View

    template: require './templates/racks'

    initialize: ->
        @racksActive = new Racks [], rackType: 'active'
        @racksDead = new Racks [], rackType: 'dead'
        @racksDecomissioning = new Racks [], rackType: 'decomissioning'

    fetch: ->
        promises = []
        promises.push @racksActive.fetch()
        promises.push @racksDead.fetch()
        promises.push @racksDecomissioning.fetch()
        $.when(promises...)

    refresh: ->
        return @ if @$el.find('[data-sorted-direction]').length

        @fetchDone = false
        @fetch().done =>
            @fetchDone = true
            @render()

        @

    render: ->
        context =
            fetchDone: @fetchDone
            racksActive: _.pluck(@racksActive.models, 'attributes')
            racksDead: _.pluck(@racksDead.models, 'attributes')
            racksDecomissioning: _.pluck(@racksDecomissioning.models, 'attributes')

        @$el.html @template context

        @setupEvents()

        utils.setupSortableTables()

        @

    setupEvents: ->
        $removeLinks = @$el.find('[data-action="remove"]')

        $removeLinks.unbind('click').on 'click', (e) =>
            $row = $(e.target).parents('tr')
            rackModel = @racksDead.get($(e.target).data('rack-id'))

            vex.dialog.confirm
                message: "<p>Are you sure you want to delete the rack?</p><pre>#{ rackModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    rackModel.destroy()
                    @racksDead.remove(rackModel)
                    $row.remove()

module.exports = RacksView