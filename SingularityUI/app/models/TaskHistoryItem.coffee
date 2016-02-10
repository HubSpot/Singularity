Model = require './model'

# This model hits up the history API and gets us the record for
# an old (or current) Task
class TaskHistoryItem extends Model

    url: -> 
        if @requestID and @deployId
            return "#{ config.apiRoot }/history/request/#{ @requestId }/deploy/#{ @deployId }/tasks/inactive"
        else if @requestId
            return "#{ config.apiRoot }/history/request/#{ @requestID }/tasks"
        else
            return "#{ config.apiRoot }/history/tasks"

    ignoreAttributes: ['id', 'canBeRunNow']

    initialize: ({ @taskId, @updatedAt, @lastTaskState }) ->
    

module.exports = TaskHistoryItem
