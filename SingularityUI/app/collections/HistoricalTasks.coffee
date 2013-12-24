RequestTasks = require './RequestTasks'

class HistoricalTasks extends Mixen(RequestTasks, Teeble.ServerCollection)

    model: Backbone.Model

    url: ->
        sortDirection = @sortDirection.toUpperCase()
        if @sortColumn in ['updatedAt', 'createdAt']
            sortDirection = if sortDirection is 'ASC' then 'DESC' else 'ASC'
        "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/request/#{ @requestId }/tasks?count=#{ @perPage }&page=#{ @currentPage }&orderBy=#{ @sortColumn }&orderDirection=#{ sortDirection }"

module.exports = HistoricalTasks
