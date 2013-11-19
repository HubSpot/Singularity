View = require './view'

Slaves = require '../collections/Slaves'

class SlavesView extends View

    template: require './templates/slaves'

    initialize: =>
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

    render: =>
        return unless @fetchDone

        context =
            slavesActive: @slavesActive.toJSON()
            slavesDead: @slavesDead.toJSON()
            slavesDecomissioning: @slavesDecomissioning.toJSON()

        @$el.html @template context

        utils.setupSortableTables()

module.exports = SlavesView