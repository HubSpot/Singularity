PaginableCollection = require './PaginableCollection'

class DeployHistory extends PaginableCollection

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/deploys"

    comparator: undefined

    initialize: (models, { @requestId }) =>

    parse: (data) ->
        for deploy in data
            deploy.id = deploy.deployMarker.deployId
        data

module.exports = DeployHistory
