Model = require './model'

class RequestDeployStatus extends Model
    propertyFilters: ['deployResult']

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/deploy/#{ @deployId }"

    initialize: ({@requestId, @deployId}) =>

module.exports = RequestDeployStatus
