Model = require './model'

class Rack extends Model

    url: => "#{ config.apiRoot }/racks/rack/#{ @get('id') }/#{ @get('rackType') }"

module.exports = Rack