Tasks = require './Tasks'

class TasksCleaning extends Tasks

    url: "#{ config.apiRoot }/tasks/cleaning"

    parse: (tasks) ->
        _.each tasks, (task, i) =>
            task.JSONString = utils.stringJSON task
            task.id = task.taskId.id
            task.name = task.id
            task.timestampHuman = moment(task.timestamp).fromNow()
            task.cleanupTypeHuman = constants.taskCleanupTypes[task.cleanupType] or ''
            tasks[i] = task
            app.allTasks[task.id] = task

        tasks

module.exports = TasksCleaning