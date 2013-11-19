Collection = require './collection'

class RequestTasks extends Collection

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/history/request/#{ @requestId }/tasks"

    initialize: (models, { @requestId }) =>

    parse: (tasks) ->
        _.each tasks, (task) ->
            task.id = task.taskDetails.id
            task.name = task.id
            task.updateAtHuman = if task.updateAt? then moment(task.updateAt).from() else ''
            task.createdAtHuman = if task.createdAt? then moment(task.createdAt).from() else ''
            task.lastStatus = task.lastStatus ? ''

        tasks

module.exports = RequestTasks