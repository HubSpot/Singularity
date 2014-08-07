Collection = require './collection'

class RequestTasks extends Collection

    url: => "#{ config.apiRoot }/history/request/#{ @requestId }/tasks/#{ @state }"

    initialize: (models, {@requestId, @state}) =>

    parse: (data) ->
        for task in data
            task.id = task.taskId.id
        data

module.exports = RequestTasks
