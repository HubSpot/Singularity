Model = require './model'

class S3Log extends Model

    parse: (logJSON) ->
        logJSON.id = logJSON.key

        logJSON.shortKey = @_truncateToShortKey logJSON.key
        logJSON.url = logJSON.getUrl
        logJSON.lastModifiedHuman = utils.humanTimeAgo logJSON.lastModified
        logJSON.sizeHuman = utils.humanizeFileSize logJSON.size

        logJSON

    _truncateToShortKey: (key) ->
        key.substring(key.lastIndexOf('/') + 1)  # gets string after last slash. returns entire string if no slashes.

module.exports = S3Log
