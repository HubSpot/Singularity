Model = require './model'

class S3Log extends Model

  parse: (logJSON) ->
    logJSON.id = logJSON.key

    logJSON.shortKey = @_truncateToShortKey logJSON.key
    logJSON.url = logJSON.getUrl
    logJSON.lastModifiedHuman = utils.humanTimeAgo logJSON.lastModified
    logJSON.sizeHuman = Humanize.fileSize logJSON.size

    logJSON

  _truncateToShortKey: (key) ->
    indexOfLastSlashOrStringLength = key.lastIndexOf('/')
    indexOfLastSlashOrStringLength = undefined if indexOfLastSlashOrStringLength is -1

    key.slice 0, indexOfLastSlashOrStringLength


module.exports = S3Log

