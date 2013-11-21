Model = require './model'

BufferWrites = require '../mixins/BufferWrites'

class Webhook extends Mixen(BufferWrites, Model)

    url: -> "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/webhooks"

    toJSON: -> @get('url')

module.exports = Webhook