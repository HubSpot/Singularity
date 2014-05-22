Collection = require './collection'
Webhook = require '../models/Webhook'

class Webhooks extends Collection

    model: Webhook

    url: "#{ window.singularity.config.apiBase }/webhooks"

    parse: (webhooks) =>
        _.map webhooks, (webhook) ->
            id: webhook
            url: webhook

    comparator: 'url'

module.exports = Webhooks