ServerItem = require './ServerItem'


class Slave extends ServerItem

    type: 'slave'

    url: => "#{ config.apiRoot }/slaves/slave/#{ @get('id') }"

module.exports = Slave
