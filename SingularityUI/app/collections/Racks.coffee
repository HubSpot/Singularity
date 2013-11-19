Collection = require './collection'

class Racks extends Collection

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/racks/#{ @rackType }"

    initialize: (models, { @rackType }) =>

    parse: (racks) ->
        _.each racks, (rackString, i) ->
            rack = {}
            rack.rack = rackString
            racks[i] = rack

        racks

module.exports = Racks