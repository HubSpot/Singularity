Collection = require './collection'
Rack = require '../models/Rack'

class Racks extends Collection

    model: Rack

    url: => "#{ config.apiBase }/racks/#{ @rackType }"

    initialize: (models, { @rackType }) =>

    parse: (racks) ->
        _.map racks, (rack) =>
            rack.rackType = @rackType
            return rack

module.exports = Racks
