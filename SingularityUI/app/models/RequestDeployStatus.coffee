Model = require './model'

class RequestDeployStatus extends Model
    propertyFilters: ['deployResult']

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/deploy/#{ @deployId }"

    initialize: ({@requestId, @deployId}) =>

    parse: (data) ->
        data.id = "#{ data.deployMarker.requestId }-#{ data.deployMarker.deployId }-#{ data.deployMarker.timestamp }"

module.exports = RequestDeployStatus
