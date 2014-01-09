RequestTasks = require './RequestTasks'

class HistoricalTasks extends Mixen(RequestTasks, Teeble.ServerCollection)

    model: Backbone.Model

    url: ->
        sortDirection = @sortDirection.toUpperCase()
        if @sortColumn in ['updatedAt', 'createdAt']
            sortDirection = if sortDirection is 'ASC' then 'DESC' else 'ASC'

        params =
            count: @perPage
            page: @currentPage
            orderBy: @sortColumn
            orderDirection: sortDirection

        "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/request/#{ @requestId }/tasks?#{ $.param params }"

module.exports = HistoricalTasks
