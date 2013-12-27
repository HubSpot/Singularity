View = require './view'

Slaves = require '../collections/Slaves'

class SlavesView extends View

    template: require './templates/slaves'

    initialize: ->
        promises = []

        @slavesActive = new Slaves [], slaveType: 'active'
        promises.push @slavesActive.fetch()

        @slavesDead = new Slaves [], slaveType: 'dead'
        promises.push @slavesDead.fetch()

        @slavesDecomissioning = new Slaves [], slaveType: 'decomissioning'
        promises.push @slavesDecomissioning.fetch()

        $.when(promises...).done =>
            @fetchDone = true
            @render()

    render: ->
        return unless @fetchDone

        context =
            slavesActive: _.pluck(@slavesActive.models, 'attributes')
            slavesDead: _.pluck(@slavesDead.models, 'attributes')
            slavesDecomissioning: _.pluck(@slavesDecomissioning.models, 'attributes')

        @$el.html @template context

        @setupEvents()

        utils.setupSortableTables()

    setupEvents: ->
        $removeLinks = @$el.find('[data-action="remove"]')

        $removeLinks.unbind('click').on 'click', (e) =>
            row = $(e.target).parents('tr')
            slaveModel = @slavesDead.get($(e.target).data('slave-id'))

            vex.dialog.confirm
                message: "<p>Are you sure you want to delete the slave:</p><pre>#{ slaveModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    slaveModel.destroy()
                    @slavesDead.remove(slaveModel)
                    row.remove()

module.exports = SlavesView