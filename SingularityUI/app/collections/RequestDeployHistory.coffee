PaginableCollection = require './PaginableCollection'

class DeployHistory extends PaginableCollection

    url: -> "#{ config.apiRoot }/history/request/#{ @requestId }/deploys"

    model: class Deploy extends Backbone.Model
        idAttribute: "deployId"
        
    comparator: undefined

    initialize: (models, { @requestId }) =>

module.exports = DeployHistory
