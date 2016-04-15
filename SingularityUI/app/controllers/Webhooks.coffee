Controller = require './Controller'

WebhooksView = require '../views/Webhooks'

WebhookSummaries = require '../collections/WebhookSummaries'

class WebhooksController extends Controller

    initialize: ->
        @title 'Webhooks'
        @collections.webhooks = new WebhookSummaries []

        @setView new WebhooksView
            collections: @collections
            fetched: false

        @collections.webhooks.fetch().done =>
            app.showView @view
            @view.fetched = true

    refresh: ->
        @collections.webhooks.fetch().done =>
            @view.fetched = true

module.exports = WebhooksController
