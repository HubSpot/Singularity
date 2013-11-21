Model = require './model'

BufferWrites = require '../mixins/BufferWrites'

class Webhook extends Mixen(BufferWrites, Model)

    toJSON: -> @get('url')

module.exports = Webhook