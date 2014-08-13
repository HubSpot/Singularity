Model = require './model'

class Deploy extends Model

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/deploy/#{ @deployId }"

    initialize: (attributes, {@requestId, @deployId}) ->

module.exports = Deploy
