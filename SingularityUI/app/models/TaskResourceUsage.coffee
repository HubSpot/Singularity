Model = require './model'

class TaskResourceUsage extends Model

    url: => "#{ config.apiRoot }/tasks/task/#{ @taskId }/statistics"

    initialize: ({ @taskId }) =>

    parse: (data) ->
        if data.memAnonBytes
            data.memAnonHuman = utils.humanizeFileSize data.memAnonBytes
        if data.memFileBytes
            data.memFileHuman = utils.humanizeFileSize data.memFileBytes
        if data.memLimitBytes
            data.memLimitHuman = utils.humanizeFileSize data.memLimitBytes
        if data.memMappedFileBytes
            data.memMappedFileHuman = utils.humanizeFileSize data.memMappedFileBytes
        if data.memRssBytes
            data.memRssHuman = utils.humanizeFileSize data.memRssBytes

        data

module.exports = TaskResourceUsage
