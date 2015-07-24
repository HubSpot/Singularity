Model = require './model'
Request = require './Request'

class HealthCheckResult extends Model

    url: => "#{ config.apiRoot }/history/task/#{ @taskId }"

    initialize: ({@taskId}) =>

    parse: (task) ->
        latest =  _.last(task.healthcheckResults)
        if latest
            # @id = latest.taskId.id
            # @hasData = true
            latest.hasData = true
            latest
        else
            latest.hasData = false

module.exports = HealthCheckResult
