Model = require './model'

# This model hits up the history API and gets us the record for
# an old (or current) Task
class TaskHistoryItem extends Model

    ignoreAttributes: ['id', 'canBeRunNow']

    url: () ->
        if @requestId and @runId
            "#{ config.apiRoot }/history/request/#{ @requestId }/run/#{ @runId }"
        else
            # Currently the above URL is the ONLY place to fetch this model.
            # If you don't have access to request ID and run ID use the TaskHistory
            # model instead.
            throw new Error """
                    Insufficient data for a meaningful answer.
                    Cannot fetch individual TaskHistoryItem without #{'requestId' unless @requestId}#{if @requestId or @runId then '' else ' and '}#{if @runId then '' else 'runId'}.
                """

    initialize: ({ @taskId, @updatedAt, @lastTaskState, @requestId, @runId }) ->
    

module.exports = TaskHistoryItem
