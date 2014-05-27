Model = require './model'

class Slave extends Model

    url: => "#{ config.apiRoot }/slaves/slave/#{ @get('id') }/#{ @get('slaveType') }"

module.exports = Slave