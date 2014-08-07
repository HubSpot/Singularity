ServerItem = require './ServerItem'

class Rack extends ServerItem

    type: 'rack'

    url: => "#{ config.apiRoot }/racks/rack/#{ @get('id') }"

module.exports = Rack
