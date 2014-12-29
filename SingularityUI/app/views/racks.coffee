View = require './view'

Rack = require '../models/Rack'

class RacksView extends View

    template: require '../templates/racks/base'

    events: =>
        _.extend super,
            'click [data-action="remove"]': 'removeRack'

    render: ->
        @$el.html @template()

        @$('#racks').html  @subviews.racks.$el

    removeRack: (event) ->
        $target = $(event.target)
        state = $target.data 'state'
        rackModel = new Rack
            id:    $target.data 'rack-id'
            state: state

        rackModel.promptRemove => @trigger 'refreshrequest'

module.exports = RacksView
