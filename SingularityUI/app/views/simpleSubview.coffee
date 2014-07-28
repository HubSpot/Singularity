View = require './view'

# You feed it a collection & template and it listens to it and renders
# when appropriate
#
#   myView = new SimpleSubview {collection, template}
#   @$('#my-container').html myView.$el
#
# And it does everything for you, just do stuff with the collection
class SimpleSubview extends View

    initialize: ({@template}) ->
        @listenTo @collection, 'sync',    @render
        @listenTo @collection, 'add',     @render
        @listenTo @collection, 'remove',  @render
        @listenTo @collection, 'reset',   =>
            @$el.empty()

    render: ->
        return if not @collection.synced and @collection.isEmpty()
        
        @$el.html @template
            data:   @collection.toJSON()
            synced: @collection.synced

module.exports = SimpleSubview
