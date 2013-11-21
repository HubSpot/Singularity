Collection = require './collection'

class Slaves extends Collection

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/slaves/#{ @slaveType }"

    initialize: (models, { @slaveType }) =>

module.exports = Slaves