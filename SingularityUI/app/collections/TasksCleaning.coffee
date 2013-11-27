TasksActive = require './TasksActive'

class TasksCleaning extends TasksActive

    url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/tasks/cleaning"

    parse: (tasks) ->
        _.each tasks, (task, i) =>
            task.JSONString = utils.stringJSON task
            task.name = task.id
            task.timestamp = task.pendingTaskId.timestamp
            task.timestampHuman = moment(task.timestamp).fromNow()
            task.cleanupTypeHuman = if constants.taskCleanupType[task.cleanupType] then constants.taskCleanupType[task.cleanupType].label else ''
            tasks[i] = task
            app.allTasks[task.id] = task

        tasks

module.exports = TasksCleaning