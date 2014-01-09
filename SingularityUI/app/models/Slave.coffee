Model = require './model'

class Slave extends Model

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/slaves/slave/#{ @get('id') }/#{ @get('slaveType') }"

module.exports = Slave