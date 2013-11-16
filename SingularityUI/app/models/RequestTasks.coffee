Model = require './model'

class RequestTasks extends Model

    url: => "http://#{ env.SINGULARITY_BASE }/#{ constants.api_base }/history/request/#{ @requestId }/tasks"

    initialize: =>
        @requestId = @attributes.requestId
        delete @attributes.requestId

    parse: (tasks) ->
        _.each tasks, (task) ->
            task.id = "#{ task.requestId }-#{ task.startedAt }-#{ task.instanceNo }-#{ task.rackId }"
            task.name = task.id

        tasks

module.exports = RequestTasks