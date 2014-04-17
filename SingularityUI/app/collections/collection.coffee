module.exports = class Collection extends Backbone.Collection

    initialize: ->
        @on 'reset', => @sort() if @comparator?
        @on 'sync', => @synced = true