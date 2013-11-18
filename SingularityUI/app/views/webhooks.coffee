View = require './view'

class WebhooksView extends View

    template: require './templates/webhooks'

    render: =>
        @$el.html @template webhooks: app.collections.webhooks.toJSON()

module.exports = WebhooksView