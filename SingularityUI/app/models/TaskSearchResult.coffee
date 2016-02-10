Model = require './model'

# This model hits up the history API and gets us the record for
# an old (or current) Task
class TaskSearchResult extends Model

    url: -> 
        if @requestID
            return "#{ config.apiRoot }/history/request/#{ @requestID }/tasks"
        else
            return "#{ config.apiRoot }/history/tasks"

    initialize: ({ @taskId, @updatedAt, @lastTaskState }) ->
    

module.exports = TaskSearchResult
