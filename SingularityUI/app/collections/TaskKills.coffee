Collection = require './collection'

TaskKill = require '../models/TaskKill'

class TaskKills extends Collection

    model: TaskKill

    comparator: 'timestamp'

    url: -> "#{ config.apiRoot }/tasks/killed"

module.exports = TaskKills
