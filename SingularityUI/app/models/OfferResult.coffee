Model = require './model'

class OfferResult extends Model

    parse: (data) ->
        data.id = data.taskId.id
        return data

module.exports = OfferResult
