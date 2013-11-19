View = require './view'

Webhooks = require '../collections/Webhooks'

class WebhooksView extends View

    template: require './templates/webhooks'

    initialize: =>
        @webhooks = new Webhooks
        @webhooks.fetch().done => @render()

    render: =>
        @$el.html @template webhooks: @webhooks.toJSON()

module.exports = WebhooksView