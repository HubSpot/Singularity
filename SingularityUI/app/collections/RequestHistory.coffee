PaginableCollection = require './PaginableCollection'

Model = require '../models/model'

# I didn't name it! This is a collection that returns a bunch
# of logs detailing what's been going on with the request.
#
# Eg: Created by sbacanu
class RequestHistory extends PaginableCollection

    model: class RequestHistoryItem extends Model
        idAttribute: "createdAt"

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/requests"

    comparator: (r0, r1) => r1.get("createdAt") - r0.get("createdAt")

    initialize: (models, { @requestId }) =>

    fetch: (params = {}) ->
        params = _.extend params,
            data: _.extend params.data or {},
                orderDirection: 'DESC'
        super params

module.exports = RequestHistory
