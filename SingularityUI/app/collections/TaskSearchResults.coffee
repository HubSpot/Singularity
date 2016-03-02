Collection = require './collection'

TaskHistoryItem = require '../models/TaskHistoryItem'

class TaskSearchResults extends Collection

    model: TaskHistoryItem

    getQueryParams: ->
        return $.param(_.pick(@params, _.identity))

    url: -> 
        if @params.requestId
            return "#{ config.apiRoot }/history/request/#{ @params.requestId }/tasks?#{ @getQueryParams() }"
        else
            return "#{ config.apiRoot }/history/tasks?#{ @getQueryParams() }"

    initialize: (models = [], { @params }) ->


module.exports = TaskSearchResults
