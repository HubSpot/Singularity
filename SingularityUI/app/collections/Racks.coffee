Collection = require './collection'

Rack = require '../models/Rack'

class Racks extends Collection

    model: Rack

    url: => "#{ config.apiRoot }/racks"

    initialize: (models) =>

    parse: (racks) ->
        _.map racks, (rack) =>
            rack

module.exports = Racks
