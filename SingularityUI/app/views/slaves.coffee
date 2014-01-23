View = require './view'

Slaves = require '../collections/Slaves'

class SlavesView extends View

    template: require './templates/slaves'

    initialize: ->
        @slavesActive = new Slaves [], slaveType: 'active'
        @slavesDead = new Slaves [], slaveType: 'dead'
        @slavesDecomissioning = new Slaves [], slaveType: 'decomissioning'

        @fetch()

    fetch: ->
        promises = []
        promises.push @slavesActive.fetch()
        promises.push @slavesDead.fetch()
        promises.push @slavesDecomissioning.fetch()
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
            slavesActive: _.pluck(@slavesActive.models, 'attributes')
            slavesDead: _.pluck(@slavesDead.models, 'attributes')
            slavesDecomissioning: _.pluck(@slavesDecomissioning.models, 'attributes')

        @$el.html @template context

        @setupEvents()

        utils.setupSortableTables()

        @

    setupEvents: ->
        $removeLinks = @$el.find('[data-action="remove"]')

        $removeLinks.unbind('click').on 'click', (e) =>
            $row = $(e.target).parents('tr')
            slaveModel = @slavesDead.get($(e.target).data('slave-id'))

            vex.dialog.confirm
                message: "<p>Are you sure you want to delete the slave:</p><pre>#{ slaveModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    slaveModel.destroy()
                    @slavesDead.remove(slaveModel)
                    $row.remove()

module.exports = SlavesView