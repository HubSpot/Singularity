Collection = require './collection'

TaskCleanup = require '../models/TaskCleanup'

class TaskCleanups extends Collection

    model: TaskCleanup

    comparator: 'timestamp'

    url: -> "#{ config.apiRoot }/tasks/cleaning"

module.exports = TaskCleanups
