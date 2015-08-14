RequestTasks = require './RequestTasks'
Collection = require './collection'

class DeployActiveTasks extends Collection

    model: class TaskHistoryItem extends Backbone.Model
      ignoreAttributes: ['id']

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/deploy/#{ @deployId }/tasks/active"

    initialize: (models, {@requestId, @deployId}) ->

    comparator: (task0, task1) =>
      -(task0.get("updatedAt") - task1.get("updatedAt"))

    parse: (data) ->
      for task in data
        task.id = task.taskId.id
      data

module.exports = DeployActiveTasks
