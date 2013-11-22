Collection = require './collection'
Webhook = require '../models/Webhook'

class Webhooks extends Collection

    model: Webhook

    url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/webhooks"

    parse: (webhooks) =>
        _.map webhooks, (webhook) ->
            id: webhook
            url: webhook

    comparator: 'url'

module.exports = Webhooks