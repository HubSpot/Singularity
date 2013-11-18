Collection = require './collection'

class TaskUpdates extends Collection

    url: -> "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/history/task/#{ @taskId }"

    initialize: (models, { @taskId }) =>

    parse: (history) ->
        _.each history.taskUpdates, (taskUpdate, i) =>
            taskUpdate.statusUpdateHuman = @parseTaskUpdateState taskUpdate.statusUpdate
            taskUpdate.statusMessage = taskUpdate.statusMessage ? 'No status message available'
            taskUpdate.timestampHuman = moment(taskUpdate.timestamp).from()

        history.taskUpdates

    parseTaskUpdateState: (state) ->
        switch state
            when 'TASK_FAILED' then 'Failed'
            when 'TASK_FINISHED' then 'Finished'
            when 'TASK_KILLED' then 'Killed'
            when 'TASK_LOST' then 'Lost'
            when 'TASK_RUNNING' then 'Running'
            when 'TASK_STAGING' then 'Staging'
            when 'TASK_STARTING' then 'Starting'
            else ''

module.exports = TaskUpdates