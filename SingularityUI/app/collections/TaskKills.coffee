Collection = require './collection'

TaskKillRecord = require '../models/TaskKillRecord'

class TaskKills extends Collection

    model: TaskKillRecord

    comparator: 'timestamp'

    url: -> "#{ config.apiRoot }/tasks/killed"

module.exports = TaskKills
