Model = require './model'

class RequestTasks extends Model

    url: -> "http://#{env.SINGULARITY_BASE}/#{constants.api_base}/history/request/#{@get('name')}"

    parse: (tasks) ->
        _.each tasks, (task) ->
            task.id = "#{ task.name }-#{ task.startedAt }-#{ task.instanceNo }-#{ task.rackId }"

        tasks

module.exports = RequestTasks