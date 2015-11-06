RequestTasks = require './RequestTasks'
PaginableCollection = require './PaginableCollection'

class DeployHistoricalTasks extends PaginableCollection

    model: class TaskHistoryItem extends Backbone.Model
      ignoreAttributes: ['id']

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/deploy/#{ @deployId }/tasks/inactive"

    initialize: (models, {@requestId, @deployId}) ->

    comparator: (task0, task1) =>
      -(task0.get("updatedAt") - task1.get("updatedAt"))

    parse: (data) ->
      for task in data
          task.id = task.taskId.id
      data

module.exports = DeployHistoricalTasks
