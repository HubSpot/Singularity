Model = require './model'

class Rack extends Model

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/racks/rack/#{ @get('id') }/#{ @get('rackType') }"

module.exports = Rack