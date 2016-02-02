Collection = require './collection'

TaskSearchResult = require '../models/TaskSearchResult'

class TaskSearchResults extends Collection

    model: TaskSearchResult

    url: -> 
    	if @requestId
    		return "#{ config.apiRoot }/request/#{ @requestID }/tasks"
    	else
    		return "#{ config.apiRoot }/tasks"

    initialize: (models = [], { @requestId, @deployId, @host, @lastTaskStatus, @startedAfter, @startedBefore, @orderDirection, @count, @page }) ->

    parse: (response) ->
    	response.results

module.exports = TaskSearchResults