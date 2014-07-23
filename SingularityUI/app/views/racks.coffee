View = require './view'

Rack = require '../models/Rack'
Racks = require '../collections/Racks'

class RacksView extends View

    template: require '../templates/racks'

    events: =>
        _.extend super, 
            'click [data-action="remove"]': 'removeRack'
            'click [data-action="decomission"]': 'decomissionRack'

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

    refresh: =>
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
        @

    removeRack: (event) ->
        $target = $(event.target)

        rackModel = new Rack
            id: $target.data 'rack-id'
            state: $target.data 'state'
        rackModel.promptRemove => @refresh()

    decomissionRack: (event) ->
        $target = $(event.target)

        rackModel = new Rack
            id: $target.data 'rack-id'
        rackModel.promptDecommission => @refresh()

module.exports = RacksView