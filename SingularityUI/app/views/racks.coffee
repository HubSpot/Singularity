View = require './view'

Rack = require '../models/Rack'

class RacksView extends View

    template: require '../templates/racks/base'

    events: =>
        _.extend super, 
            'click [data-action="remove"]': 'removeRack'
            'click [data-action="decomission"]': 'decomissionRack'

    render: ->
        @$el.html @template()

        @$('#active').html         @subviews.activeRacks.$el
        @$('#dead').html           @subviews.deadRacks.$el
        @$('#decomissioning').html @subviews.decomissioningRacks.$el

    removeRack: (event) ->
        $target = $(event.target)

        rackModel = new Rack
            id: $target.data 'rack-id'
            state: $target.data 'state'
        rackModel.promptRemove => @trigger 'refreshrequest'

    decomissionRack: (event) ->
        $target = $(event.target)

        rackModel = new Rack
            id: $target.data 'rack-id'
        rackModel.promptDecommission => @trigger 'refreshrequest'

module.exports = RacksView