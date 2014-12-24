ServerItem = require './ServerItem'


class Slave extends ServerItem

    type: 'slave'

    url: => "#{ config.apiRoot }/slave/#{ @get('id') }"

module.exports = Slave
