View = require './view'

class SimpleTableView extends View

    initialize: ({@template}) ->
        @listenTo @collection, 'sync',  @render
        @listenTo @collection, 'reset', =>
            @$el.empty()

    render: ->
        return if not @collection.synced
        
        @$el.html @template
            data:   @collection.toJSON()
            synced: @collection.synced

module.exports = SimpleTableView
