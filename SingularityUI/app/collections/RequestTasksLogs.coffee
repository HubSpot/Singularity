S3Log = require '../models/S3Log'

Collection = require './collection'
ClientsidePaginableCollection = require './ClientsidePaginableCollection'

class RequestTasksLogs extends ClientsidePaginableCollection

    model: S3Log

    url: => "#{ config.apiRoot }/logs/request/#{ @requestId }"

    initialize: (models, {@requestId}) =>

module.exports = RequestTasksLogs