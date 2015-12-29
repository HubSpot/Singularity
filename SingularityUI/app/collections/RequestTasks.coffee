Collection = require './collection'
PaginableCollection = require './PaginableCollection'

class RequestTasks extends PaginableCollection

    url: => "#{ config.apiRoot }/history/request/#{ @requestId }/tasks/#{ @state }"

    initialize: (models, {@requestId, @state}) =>

    parse: (data) ->
        for task in data
            task.id = task.taskId.id
            task.deployId = task.taskId.deployId
        data

    getTasksForDeploy: (deployId) ->
        @filter((task) =>
            task.get('taskId').deployId == deployId)

module.exports = RequestTasks
