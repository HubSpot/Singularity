Collection = require './collection'

class TaskUpdates extends Collection

    url: -> "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/history/task/#{ @taskId }"

    initialize: (models, { @taskId }) =>

    parse: (history) ->
        _.each history.taskUpdates, (taskUpdate, i) =>
            taskUpdate.statusUpdateHuman = if constants.taskStates[taskUpdate.statusUpdate] then constants.taskStates[taskUpdate.statusUpdate].label else ''
            taskUpdate.statusMessage = taskUpdate.statusMessage ? 'No status message available'
            taskUpdate.timestampHuman = moment(taskUpdate.timestamp).from()

        history.taskUpdates

module.exports = TaskUpdates