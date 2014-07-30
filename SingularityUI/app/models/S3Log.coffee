Model = require './model'

class S3Log extends Model

    idAttribute: 'key'

    parse: (logJSON) ->
        logJSON.shortKey = @_truncateToShortKey logJSON.key
        logJSON

    _truncateToShortKey: (key) ->
        key.substring(key.lastIndexOf('/') + 1)  # gets string after last slash. returns entire string if no slashes.

module.exports = S3Log
