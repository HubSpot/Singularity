Model = require '../models/model'

# Base collection extended by the others
class Collection extends Backbone.Collection

    model: Model

    # Tracks if the collection has synced
    synced: false

    constructor: ->
        super
        @on 'sync',  =>
            @synced = true
            @each (model) => model.synced = true
        @on 'reset', => @synced = false

module.exports = Collection
