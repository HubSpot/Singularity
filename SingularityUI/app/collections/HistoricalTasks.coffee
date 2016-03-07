PaginableCollection = require './PaginableCollection'
TaskHistoryItem = require '../models/TaskHistoryItem'

class HistoricalTasks extends PaginableCollection

    model: TaskHistoryItem

    getQueryParams: ->
        return $.param(_.pick(@params, _.identity))

    url: -> 
        if @params.requestId
            return "#{ config.apiRoot }/history/request/#{ @params.requestId }/tasks?#{ @getQueryParams() }"
        else
            return "#{ config.apiRoot }/history/tasks?#{ @getQueryParams() }"

    initialize: (models = [], { @params }) ->

    comparator: (task0, task1) =>
      -(task0.get("updatedAt") - task1.get("updatedAt"))

    parse: (data) ->
      for task in data
          task.id = task.taskId.id
      data


module.exports = HistoricalTasks
