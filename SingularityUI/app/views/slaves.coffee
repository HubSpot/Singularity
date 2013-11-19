View = require './view'

Slaves = require '../collections/Slaves'

class SlavesView extends View

    template: require './templates/slaves'

    initialize: =>
        @slavesActive = new Slaves [], slaveType: 'active'
        @slavesActive.fetch().done => @render()

        @slavesDead = new Slaves [], slaveType: 'dead'
        @slavesDead.fetch().done => @render()

        @slavesDecomissioning = new Slaves [], slaveType: 'decomissioning'
        @slavesDecomissioning.fetch().done => @render()

    render: =>
        context =
            slavesActive: @slavesActive.toJSON()
            slavesDead: @slavesDead.toJSON()
            slavesDecomissioning: @slavesDecomissioning.toJSON()

        @$el.html @template context

module.exports = SlavesView