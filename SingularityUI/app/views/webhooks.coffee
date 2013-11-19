View = require './view'

Webhooks = require '../collections/Webhooks'

class WebhooksView extends View

    template: require './templates/webhooks'

    initialize: =>
        @webhooks = new Webhooks
        @webhooks.fetch().done =>
            @fetchDone = true
            @render()

    render: =>
        return unless @fetchDone

        @$el.html @template webhooks: @webhooks.toJSON()
        utils.setupSortableTables()

module.exports = WebhooksView