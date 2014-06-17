Model = require './model'

class TaskStatistics extends Model

    url: => "#{ config.apiRoot }/tasks/task/#{ @taskId }/statistics"

    initialize: (models, { @taskId }) =>

    parse: (data) ->
        if data.memAnonBytes
            data.memAnonHuman = Humanize.fileSize data.memAnonBytes
        if data.memFileBytes
            data.memFileHuman = Humanize.fileSize data.memFileBytes
        if data.memLimitBytes
            data.memLimitHuman = Humanize.fileSize data.memLimitBytes
        if data.memMappedFileBytes
            data.memMappedFileHuman = Humanize.fileSize data.memMappedFileBytes
        if data.memRssBytes
            data.memRssHuman = Humanize.fileSize data.memRssBytes

        data


module.exports = TaskStatistics