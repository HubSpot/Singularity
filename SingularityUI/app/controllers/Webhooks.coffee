Controller = require './Controller'

WebhooksView = require '../views/Webhooks'

WebhookSummaries = require '../collections/WebhookSummaries'

class WebhooksController extends Controller

    initialize: ->
        @title 'Webhooks'
        @collections.webhooks = new WebhookSummaries []

        @setView new WebhooksView
            collections: @collections
            fetchedWebhooks: false

        app.showView @view
        @refresh()

    refresh: ->
        @collections.webhooks.fetch()
            .done => @view.fetchedWebhooks = true

module.exports = WebhooksController
