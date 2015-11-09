Collection = require './collection'
PaginableCollection = require './PaginableCollection'

class RequestTasks extends PaginableCollection

    url: => "#{ config.apiRoot }/history/request/#{ @requestId }/tasks/#{ @state }"

    initialize: (models, {@requestId, @state}) =>

    parse: (data) ->
        for task in data
            task.id = task.taskId.id

        data = data.sort((a, b) -> a.taskId.instanceNo > b.taskId.instanceNo)
        data

    getTasksForDeploy: (deployId) ->
        @filter((task) =>
            task.get('taskId').deployId == deployId)

module.exports = RequestTasks
