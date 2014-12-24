Collection = require './collection'
Slave = require '../models/Slave'

class Slaves extends Collection

    model: Slave

    url: => "#{ config.apiRoot }/slaves"

    initialize: (models, { @slaveStates }) =>

    parse: (slaves) ->
        _.map slaves, (slave) =>
            slave if slave.currentState.state in @slaveStates

module.exports = Slaves
