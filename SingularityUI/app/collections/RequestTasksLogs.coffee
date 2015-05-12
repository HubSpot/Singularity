S3Log = require '../models/S3Log'

Collection = require './collection'

class RequestTasksLogs extends Collection

    model: S3Log

    url: => "#{ config.apiRoot }/logs/request/#{ @requestId }"

    initialize: (models, {@requestId}) =>

module.exports = RequestTasksLogs