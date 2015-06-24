Collection = require './collection'

Deploy = require '../models/Deploy'

class Deploys extends Collection

    model: Deploy

    initialize: ({ @state }) ->
        @state = if not @state?  then '' else @state

    url: ->
        "#{ config.apiRoot }/deploys/#{ @state }"
        
module.exports = Deploys

