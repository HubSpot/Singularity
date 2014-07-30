Collection = require './collection'

class RequestTasks extends Collection

    url: => "#{ config.apiRoot }/history/request/#{ @requestId }/tasks/#{ @state }"

    comparator: -> - @get('createdAt')

    initialize: (models, { @requestId, @state, @sortColumn, @sortDirection }) => super

module.exports = RequestTasks
