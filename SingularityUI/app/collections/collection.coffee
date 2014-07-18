module.exports = class Collection extends Backbone.Collection

    # Tracks if the collection has synced
    synced: false

    constructor: ->
        super
        @on 'sync',  => @synced = true
        @on 'reset', => @synced = false
