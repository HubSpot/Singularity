Backbone = require 'backbone'

# Base model. Extend meeeeeee!!111
class Model extends Backbone.Model

    # Model keeps track of whether or not it's been fetched
    synced: false

    constructor: ->
        super
        @on 'sync', => @synced = true

module.exports = Model
