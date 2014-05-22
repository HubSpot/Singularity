Model = require './model'

class Slave extends Model

    url: => "#{ window.singularity.config.apiBase }/slaves/slave/#{ @get('id') }/#{ @get('slaveType') }"

module.exports = Slave