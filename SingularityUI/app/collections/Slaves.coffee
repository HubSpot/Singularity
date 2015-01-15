Collection = require './collection'
Slave = require '../models/Slave'

class Slaves extends Collection

    model: Slave

    url: => "#{ config.apiRoot }/slaves"

    initialize: (models) =>

    parse: (slaves) ->
        _.map slaves, (slave) =>
            slave

module.exports = Slaves
