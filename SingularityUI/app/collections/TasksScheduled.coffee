Tasks = require './Tasks'
TaskScheduled = require '../models/TaskScheduled'

class TasksScheduled extends Tasks

    model: TaskScheduled

    url: "#{ config.apiRoot }/tasks/scheduled"

    comparator: 'nextRunAt'

module.exports = TasksScheduled