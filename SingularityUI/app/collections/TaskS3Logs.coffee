S3Log = require '../models/S3Log'

ClientsidePaginableCollection = require './ClientsidePaginableCollection'

class TaskS3Logs extends ClientsidePaginableCollection
    
    model: S3Log

    url: -> "#{ config.apiRoot }/logs/task/#{ @taskId }"

    initialize: (models, { @taskId }) =>

module.exports = TaskS3Logs
