View = require './view'

class OffersView extends View

    template: require '../templates/offers'

    events: =>
        _.extend super,
            'click [data-action="viewJSON"]': 'viewJson'

    initialize: ->
        @listenTo @collection, 'sync', @render

    render: ->
        @$el.html @template
            offers: @collection.toJSON()
            synced: @collection.synced

        @$('.actions-column a[title]').tooltip()

        super.afterRender()

    viewJson: (e) ->
        id = $(e.target).data 'task-id'
        utils.viewJSON @collection.find((model) ->
            return model.get('id') == id
        )

module.exports = OffersView