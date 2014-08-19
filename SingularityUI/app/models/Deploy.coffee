Model = require './model'

# Used to POST new delays by the deploy form
class Deploy extends Model

    url: -> "#{ config.apiRoot }/requests/request/#{ @requestId }/deploy"

    isNew: -> true

    initialize: (attrs, {@requestId}) ->

module.exports = Deploy
