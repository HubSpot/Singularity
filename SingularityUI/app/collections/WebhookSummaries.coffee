Collection = require './collection'

WebhookSummary = require '../models/WebhookSummary'

class WebhookSummaries extends Collection

    model: WebhookSummary

    url: ->
        "#{ config.apiRoot }/webhooks/summary"

module.exports = WebhookSummaries
