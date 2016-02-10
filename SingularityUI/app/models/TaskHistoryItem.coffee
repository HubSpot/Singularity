Model = require './model'

# This model hits up the history API and gets us the record for
# an old (or current) Task
class TaskHistoryItem extends Model

    ignoreAttributes: ['id', 'canBeRunNow']

    initialize: ({ @taskId, @updatedAt, @lastTaskState }) ->
    

module.exports = TaskHistoryItem
