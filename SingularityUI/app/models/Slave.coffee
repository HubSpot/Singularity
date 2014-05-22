Model = require './model'

class Slave extends Model

    url: => "#{ config.apiBase }/slaves/slave/#{ @get('id') }/#{ @get('slaveType') }"

module.exports = Slave