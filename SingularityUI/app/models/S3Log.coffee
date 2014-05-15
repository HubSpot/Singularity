Model = require './model'

class S3Log extends Model

  parse: (logJSON) ->
    logJSON.id = logJSON.key

    logJSON.url = logJSON.getUrl
    logJSON.lastModifiedHuman = utils.humanTimeAgo logJSON.lastModified
    logJSON.sizeHuman = Humanize.fileSize logJSON.size

    logJSON

module.exports = S3Log

