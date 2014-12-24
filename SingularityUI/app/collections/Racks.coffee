Collection = require './collection'

Rack = require '../models/Rack'

class Racks extends Collection

    model: Rack

    url: => "#{ config.apiRoot }/racks"

    initialize: (models, { @rackStates }) =>

    parse: (racks) ->
        _.map racks, (rack) =>
            rack if rack.currentState.state in @rackStates

module.exports = Racks
