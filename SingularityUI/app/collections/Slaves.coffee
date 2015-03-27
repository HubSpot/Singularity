Collection = require './collection'
Slave = require '../models/Slave'

class Slaves extends Collection

    model: Slave

    url: => "#{ config.apiRoot }/slaves"

    initialize: (models) =>

module.exports = Slaves
