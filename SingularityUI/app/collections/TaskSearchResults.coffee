Collection = require './collection'

TaskSearchResult = require '../models/TaskSearchResult'

class TaskSearchResults extends Collection

    model: TaskSearchResult

    params: ->
    	anyParams = false
    	base = '?'
    	if @deployId
    		base += 'deployId=' + @deployId
    		anyParams = true
    	if @host
    		if anyParams
    			base += '&'
    		base += 'host=' + @host
    		anyParams = true
    	if @lastTaskStatus
    		if anyParams
    			base += '&'
    		base += 'lastTaskStatus=' + @lastTaskStatus
    		anyParams = true
    	if @startedAfter
    		if anyParams
    			base += '&'
    		base += 'startedAfter=' + @startedAfter
    		anyParams = true
    	if @startedBefore
    		if anyParams
    			base += '&'
    		base += 'startedBefore=' + @startedBefore
    		anyParams = true
    	if @orderDirection
    		if anyParams
    			base += '&'
    		base += 'orderDirection=' + @orderDirection
    		anyParams = true
    	if @count
    		if anyParams
    			base += '&'
    		base += 'count=' + @count
    		anyParams = true
    	if @page
    		if anyParams
    			base += '&'
    		base += 'page=' + @page
    		anyParams = true
    	return '' if not anyParams
    	return base

    url: -> 
    	if @requestId
    		base = "#{ config.apiRoot }/history/request/#{ @requestId }/tasks"
    	else
    		base = "#{ config.apiRoot }/history/tasks"
    	return base + @params()

    initialize: (models = [], { @requestId, @deployId, @host, @lastTaskStatus, @startedAfter, @startedBefore, @orderDirection, @count, @page }) ->


module.exports = TaskSearchResults