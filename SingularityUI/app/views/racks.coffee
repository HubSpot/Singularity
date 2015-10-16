View = require './view'

Rack = require '../models/Rack'
Racks = require '../collections/Racks'

class RacksView extends View

    template: require '../templates/racks/base'
    rackTemplate: require '../templates/racks/rack'
    
    initialPageLoad: true

    initialize: ({@state}) ->
        for eventName in ['sync', 'add', 'remove', 'change']
            @listenTo @collection, eventName, @render

        @listenTo @collection, 'reset', =>
            @$el.empty()

    events: =>
        _.extend super,
            'click [data-action="remove"]':       'removeRack'
            'click [data-action="decommission"]': 'decommissionRack'
            'click [data-action="reactivate"]':   'reactivateRack'

    render: ->
        return if not @collection.synced and @collection.isEmpty?()
        @$el.html @template()

        active = new Racks(
            @collection.filter (model) ->
              model.get('state') in ['ACTIVE']
        )
        decommission = new Racks(
            @collection.filter (model) ->
              model.get('state') in ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']
        )
        inactive = new Racks(
            @collection.filter (model) ->
              model.get('state') in ['DEAD', 'MISSING_ON_STARTUP']
        )

        @$('#active').html @rackTemplate
            data:     active.toJSON()
        @$('#decommission').html @rackTemplate
            data:     decommission.toJSON()
        @$('#inactive').html @rackTemplate
            data:     inactive.toJSON()

        @$('.actions-column a[title]').tooltip()

        if @state and @initialPageLoad
            return if @state is 'all'
            utils.scrollTo "##{@state}"
            @initialPageLoad = false

        super.afterRender()

    removeRack: (event) ->
        $target = $(event.currentTarget)
        state = $target.data 'state'
        rackModel = new Rack
            id:    $target.data 'rack-id'
            host:  null
            state: state

        rackModel.promptRemove => @trigger 'refreshrequest'

    decommissionRack: (event) ->
        $target = $(event.currentTarget)
        state = $target.data 'state'
        rackModel = new Rack
            id:    $target.data 'rack-id'
            host:  null
            state: state

        rackModel.promptDecommission => @trigger 'refreshrequest'

    reactivateRack: (event) =>
        $target = $(event.currentTarget)
        state = $target.data 'state'
        rackModel = new Rack
            id:    $target.data 'rack-id'
            host:  null
            state: state

        rackModel.promptReactivate => @trigger 'refreshrequest'


module.exports = RacksView
