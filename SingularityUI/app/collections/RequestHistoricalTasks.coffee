RequestTasks = require './RequestTasks'
PaginableCollection = require './PaginableCollection'

class HistoricalTasks extends Mixen(RequestTasks, PaginableCollection)

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/tasks"

    initialize: (models, {@requestId}) ->

    comparator: (task0, task1) =>
        -(task0.get("updatedAt") - task1.get("updatedAt"))

module.exports = HistoricalTasks
