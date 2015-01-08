ServerItem = require './ServerItem'


class Slave extends ServerItem

    type: 'slave'

    url: => "#{ config.apiRoot }/slaves/slave/#{ @get('id') }"

    parse: (slave) ->
        slave[0]

module.exports = Slave
