View = require './view'

Slave = require '../models/Slave'
Slaves = require '../collections/Slaves'

class SlavesView extends View

    template: require '../templates/slaves'

    events: =>
        _.extend super,
            'click [data-action="remove"]': 'removeSlave'
            'click [data-action="decommission"]': 'decommissionSlave'

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
        @

    decommissionSlave: (event) =>
        $target = $(event.currentTarget)

        slaveModel = new Slave id: $target.data 'slave-id'
        slaveModel.promptDecommission => @refresh()

    removeSlave: (event) =>
        $target = $(event.currentTarget)

        slaveModel = new Slave
            id: $target.data 'slave-id'
            state: $target.data 'state'
        slaveModel.promptRemove => @refresh()
            

module.exports = SlavesView