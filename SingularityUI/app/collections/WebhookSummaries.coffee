Collection = require './collection'

WebhookSummary = require '../models/WebhookSummary'

class WebhookSummaries extends Collection

    model: WebhookSummary

    initialize: (models) =>

    url: ->
        "#{ config.apiRoot }/webhooks/summary"

    sortBy: (field, sortDirectionAscending) ->
        if field is 'queueSize'
            sorted = _.sortBy @models, (webhookSummary) => webhookSummary.attributes.queueSize
        else
            sorted = _.sortBy @models, (webhookSummary) => webhookSummary.attributes.webhook[field]
        sorted.reverse() unless sortDirectionAscending
        @models = sorted

module.exports = WebhookSummaries
