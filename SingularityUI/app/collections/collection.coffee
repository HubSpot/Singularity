module.exports = class Collection extends Backbone.Collection

    # Tracks if the collection has synced
    synced: false

    initialize: ->
        @on 'reset', => @sort() if @comparator?
        @on 'sync', => @synced = true