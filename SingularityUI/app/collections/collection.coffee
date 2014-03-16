module.exports = class Collection extends Backbone.Collection

    initialize: ->
        @on 'reset', @sort, @
        @on 'sync', => @synced = true