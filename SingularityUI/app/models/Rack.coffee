Model = require './model'

class Rack extends Model

    url: => "#{ config.apiBase }/racks/rack/#{ @get('id') }/#{ @get('rackType') }"

module.exports = Rack