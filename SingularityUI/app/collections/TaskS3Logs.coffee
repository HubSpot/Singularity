S3Log = require '../models/S3Log'

Collection = require './collection'

# Can't just extend Teeble.ServerCollection directly due to Mixen bugs :(
class TaskS3Logs extends Collection
    
    model: S3Log

    url: ->
        params =
            count: @perPage
            page: @currentPage

        "#{ config.apiRoot }/logs/task/#{ @taskId }?#{ $.param params }"

    initialize: ({ @taskId }) => super

module.exports = TaskS3Logs
