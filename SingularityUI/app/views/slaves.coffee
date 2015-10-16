View = require './view'

Slave = require '../models/Slave'
Slaves = require '../collections/Slaves'

class SlavesView extends View

    template: require '../templates/slaves/base'
    slaveTemplate: require '../templates/slaves/slave'

    initialPageLoad: true

    initialize: ({@state}) ->
        for eventName in ['sync', 'add', 'remove', 'change']
            @listenTo @collection, eventName, @render

        @listenTo @collection, 'reset', =>
            @$el.empty()


    events: =>
        _.extend super,
            'click [data-action="remove"]':       'removeSlave'
            'click [data-action="decommission"]': 'decommissionSlave'
            'click [data-action="reactivate"]':   'reactivateSlave'
            'click [data-action="freeze"]':       'freezeSlave'

    render: ->
        return if not @collection.synced and @collection.isEmpty?()
        @$el.html @template()

        active = new Slaves(
            @collection.filter (model) ->
              model.get('state') in ['ACTIVE']
        )
        frozen = new Slaves(
            @collection.filter (model) ->
                model.get('state') in ['FROZEN']
        )
        decommission = new Slaves(
            @collection.filter (model) ->
              model.get('state') in ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']
        )
        inactive = new Slaves(
            @collection.filter (model) ->
              model.get('state') in ['DEAD', 'MISSING_ON_STARTUP']
        )

        @$('#active').html @slaveTemplate
            data:     active.toJSON()
        @$('#frozen').html @slaveTemplate
            data:     frozen.toJSON()
        @$('#decommission').html @slaveTemplate
            data:     decommission.toJSON()
        @$('#inactive').html @slaveTemplate
            data:     inactive.toJSON()

        @$('.actions-column a[title]').tooltip()

        if @state and @initialPageLoad
            return if @state is 'all'
            utils.scrollTo "##{@state}"
            @initialPageLoad = false

        super.afterRender()

    removeSlave: (event) =>
        $target = $(event.currentTarget)
        state = $target.data 'state'
        slaveModel = new Slave
            id:    $target.data 'slave-id'
            host:  $target.data 'slave-host'
            state: state

        slaveModel.promptRemove => @trigger 'refreshrequest'

    freezeSlave: (event) =>
        $target = $(event.currentTarget)
        state = $target.data 'state'
        slaveModel = new Slave
            id:    $target.data 'slave-id'
            host:  $target.data 'slave-host'
            state: state

        slaveModel.promptFreeze => @trigger 'refreshrequest'

    decommissionSlave: (event) =>
        $target = $(event.currentTarget)
        state = $target.data 'state'
        slaveModel = new Slave
            id:    $target.data 'slave-id'
            host:  $target.data 'slave-host'
            state: state

        slaveModel.promptDecommission => @trigger 'refreshrequest'

    reactivateSlave: (event) =>
        $target = $(event.currentTarget)
        state = $target.data 'state'
        slaveModel = new Slave
            id:    $target.data 'slave-id'
            host:  $target.data 'slave-host'
            state: state

        slaveModel.promptReactivate => @trigger 'refreshrequest'

module.exports = SlavesView
