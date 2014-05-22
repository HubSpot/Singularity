Collection = require './collection'
Slave = require '../models/Slave'

class Slaves extends Collection

    model: Slave

    url: => "#{ window.singularity.config.apiBase }/slaves/#{ @slaveType }"

    initialize: (models, { @slaveType }) =>

    parse: (slaves) ->
        _.map slaves, (slave) =>
            slave.slaveType = @slaveType
            return slave

module.exports = Slaves