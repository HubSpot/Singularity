RequestTasks = require './RequestTasks'
PaginableCollection = require './PaginableCollection'

class HistoricalTasks extends PaginableCollection

    model: class TaskHistoryItem extends Backbone.Model
        ignoreAttributes: ['id', 'canBeRunNow']

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/tasks"

    initialize: (models, {@requestId}) ->

    comparator: (task0, task1) =>
        -(task0.get("updatedAt") - task1.get("updatedAt"))

    parse: (data) ->
        for task in data
            task.id = task.taskId.id
        data

    getTasksForDeploy: (deployId) ->
        @filter((task) =>
            task.get('taskId').deployId == deployId)

module.exports = HistoricalTasks
