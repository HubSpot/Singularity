Model = require './model'

class Rack extends Model

    url: => "#{ window.singularity.config.apiBase }/racks/rack/#{ @get('id') }/#{ @get('rackType') }"

module.exports = Rack