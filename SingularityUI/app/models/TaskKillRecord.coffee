Model = require './model'

class TaskKillRecord extends Model

    parse: (item) ->
        item.id = "#{ item.taskId.id }-#{ item.timestamp }"
        item

module.exports = TaskKillRecord
