RequestTasks = require './RequestTasks'

class HistoricalTasks extends Mixen(RequestTasks, Teeble.ServerCollection)
    model: Backbone.Model

    url: -> "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/request/#{ @requestId }/tasks?count=10&page=#{ @currentPage }&orderBy=updatedAt"

module.exports = HistoricalTasks
