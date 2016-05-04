Model = require './model'

class TaskId extends Model

    url: -> "#{ config.apiRoot }/requests/request/#{@requestId}/run/#{@runId}"

    initialize: ({@requestId, @runId}) ->



module.exports = TaskId
