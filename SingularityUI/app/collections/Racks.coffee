Collection = require './collection'

class Racks extends Collection

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/racks/#{ @rackType }"

    initialize: (models, { @rackType }) =>

module.exports = Racks