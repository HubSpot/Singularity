Collection = require './collection'

TaskKillRecord = require '../models/TaskKillRecord'

class TaskKillRecords extends Collection

    model: TaskKillRecord

    comparator: 'timestamp'

    url: -> "#{ config.apiRoot }/tasks/killed"

module.exports = TaskKillRecords
