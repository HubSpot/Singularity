module.exports = class Collection extends Backbone.Collection

    initialize: ->
        super
        @on 'reset', @sort, @