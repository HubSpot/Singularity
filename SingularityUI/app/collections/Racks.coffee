Collection = require './collection'

Rack = require '../models/Rack'

class Racks extends Collection

    model: Rack

    url: => "#{ config.apiRoot }/racks/#{ @rackType }"

    initialize: (models, { @rackType }) =>

    parse: (racks) ->
        _.map racks, (rack) =>
            rack.rackType = @rackType
            rack

module.exports = Racks
