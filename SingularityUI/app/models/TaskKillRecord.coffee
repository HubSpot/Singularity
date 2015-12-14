Model = require './model'

class TaskKillRecord extends Model

    url: => "#{ config.apiRoot }/tasks/task/#{ @get('id') }"

    initialize: ->

    parse: (kill) ->
        kill.isDueToDecomission = kill.taskCleanupType == 'DECOMISSIONING'

        kill.id = "#{ kill.taskId.id }-#{ kill.timestamp }"

        kill.requestId = kill.taskId?.requestId

        kill

module.exports = TaskKillRecord
