Model = require './model'
Request = require './Request'

class HealthCheckResult extends Model

    url: => "#{ config.apiRoot }/history/task/#{ @taskId }"

    initialize: ({@taskId}) =>

    parse: (task) ->
        latest =  _.last(task.healthcheckResults)
        if latest
            latest.id = latest.taskId.id
        latest

module.exports = HealthCheckResult
