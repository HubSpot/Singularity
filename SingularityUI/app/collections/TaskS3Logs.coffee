S3Log = require '../models/S3Log'

# Can't just extend Teeble.ServerCollection directly due to Mixen bugs :(
class TaskS3Logs extends Mixen(Teeble.ServerCollection)
    model: S3Log

    url: ->
        params =
            count: @perPage
            page: @currentPage

        "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/logs/task/#{ @taskId }?#{ $.param params }"

    initialize: (models, { @taskId }) =>
        super

module.exports = TaskS3Logs
