S3Log = require '../models/S3Log'

ClientsidePaginableCollection = require './ClientsidePaginableCollection'

class TaskS3Logs extends ClientsidePaginableCollection
    
    model: S3Log

    url: -> "#{ config.apiRoot }/logs/task/#{ @taskId }"

    initialize: (models, { @taskId }) =>

    parse: (model) ->
        for m in model
            m.taskId = @taskId
        model

module.exports = TaskS3Logs
