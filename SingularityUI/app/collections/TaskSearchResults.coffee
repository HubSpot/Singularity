Collection = require './collection'

TaskSearchResult = require '../models/TaskSearchResult'

class TaskSearchResults extends Collection

    model: TaskSearchResult

    url: -> 
    	if @requestId
    		return "#{ config.apiRoot }/history/request/#{ @requestId }/tasks"
    	else
    		return "#{ config.apiRoot }/history/tasks"

    initialize: (models = [], { @requestId, @deployId, @host, @lastTaskStatus, @startedAfter, @startedBefore, @orderDirection, @count, @page }) ->


module.exports = TaskSearchResults