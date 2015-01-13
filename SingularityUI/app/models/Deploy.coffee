Model = require './model'

# Used to POST new delays by the deploy form
class Deploy extends Model

    url: ->
        if @deployId
            return "#{ config.apiRoot }/history/request/#{ @requestId }/deploy/#{ @deployId }"
        else
            return "#{ config.apiRoot }/deploys"

    isNew: -> true

    initialize: (deployWrapper, {@requestId, @deployId}) ->
      

module.exports = Deploy
