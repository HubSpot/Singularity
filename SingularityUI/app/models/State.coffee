Model = require './model'

class State extends Model

    url: -> "#{ config.apiRoot }/state"

module.exports = State