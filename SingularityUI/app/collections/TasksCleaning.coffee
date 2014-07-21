Tasks = require './Tasks'

class TasksCleaning extends Tasks

    url: "#{ config.apiRoot }/tasks/cleaning"

    parse: (tasks) ->
        for task in tasks
            task.JSONString = utils.stringJSON task
            task.id = task.taskId.id
            task.name = task.id
            task.timestampHuman = moment(task.timestamp).fromNow()
            task.cleanupTypeHuman = constants.taskCleanupTypes[task.cleanupType] or ''

        tasks

module.exports = TasksCleaning