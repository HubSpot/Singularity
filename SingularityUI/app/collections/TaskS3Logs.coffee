S3Log = require '../models/S3Log'

PaginableCollection = require './PaginableCollection'

# Can't just extend Teeble.ServerCollection directly due to Mixen bugs :(
class TaskS3Logs extends PaginableCollection
    
    model: S3Log

    url: -> "#{ config.apiRoot }/logs/task/#{ @taskId }"

    initialize: (models, { @taskId }) => super

module.exports = TaskS3Logs
