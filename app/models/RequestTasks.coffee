Model = require './model'

MOCK_JSON = [{"name":"Gamera:1086:web:1384190422867","instanceNo":1,"rackId":"useast1e","startedAt":1384190423579},{"name":"Gamera:1086:web:1384190422867","instanceNo":1,"rackId":"useast1a","startedAt":1384199946028}]

class RequestTasks extends Model

    #url: -> "http://#{env.SINGULARITY_BASE}/#{constants.api_base}/history/request/#{@get('name')}"
    url: => "https://#{env.INTERNAL_BASE}/#{constants.kumonga_api_base}/users/#{app.login.context.user.email}/settings"

    parse: (tasks) ->
        tasks = MOCK_JSON

        _.each tasks, (task) ->
            task.id = "#{ task.name }-#{ task.startedAt }-#{ task.instanceNo }-#{ task.rackId }"

        tasks

module.exports = RequestTasks