Collection = require './collection'

class Webhooks extends Collection

    model: require '../models/Webhook'

    url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/webhooks"

    parse: (webhooks) =>
        _.map webhooks, (webhook) ->
            id: webhook
            url: webhook

    comparator: 'url'

module.exports = Webhooks