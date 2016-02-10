RequestTasks = require './RequestTasks'
PaginableCollection = require './PaginableCollection'
TaskHistoryItem = require '../models/TaskHistoryItem'

class DeployHistoricalTasks extends PaginableCollection

    model: TaskHistoryItem

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/deploy/#{ @deployId }/tasks/inactive"

    initialize: (models, {@requestId, @deployId}) ->

    comparator: (task0, task1) =>
      -(task0.get("updatedAt") - task1.get("updatedAt"))

    parse: (data) ->
      for task in data
          task.id = task.taskId.id
      data

module.exports = DeployHistoricalTasks
