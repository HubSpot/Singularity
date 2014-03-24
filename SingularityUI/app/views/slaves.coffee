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
        @$el.find('[data-action="remove"]').unbind('click').on 'click', (e) =>
            $row = $(e.target).parents('tr')

            collection = undefined

            if $(e.target).data('collection') is 'slavesDead'
                collection = @slavesDead
            if $(e.target).data('collection') is 'slavesDecomissioning'
                collection = @slavesDecomissioning
            return new Error('No collection specified to find the model.') unless collection

            slaveModel = collection.get($(e.target).data('slave-id'))

            vex.dialog.confirm
                buttons: [
                    $.extend({}, vex.dialog.buttons.YES, (text: 'Remove', className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'))
                    vex.dialog.buttons.NO
                ]
                message: "<p>Are you sure you want to remove the slave?</p><pre>#{ slaveModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    slaveModel.destroy()
                    collection.remove(slaveModel)
                    $row.remove()

module.exports = SlavesView