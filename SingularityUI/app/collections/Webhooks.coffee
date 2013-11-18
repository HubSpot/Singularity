Collection = require './collection'

class Webhooks extends Collection

    url: "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/webhooks"

    parse: (webhooks) =>
        _.each webhooks, (webhook, i) =>
            webhooks[i] = url: webhook

        webhooks

    comparator: 'url'

module.exports = Webhooks