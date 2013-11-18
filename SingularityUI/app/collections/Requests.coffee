Collection = require './collection'

class Requests extends Collection

    parse: (requests) ->
        _.each requests, (request, i) =>
            request.id = request.id
            request.deployUser = @parseDeployUser request
            request.JSONString = utils.stringJSON request
            request.timestampHuman = moment(request.timestamp).from()
            requests[i] = request

        requests

    parseDeployUser: (request) ->
        (request.executorData?.env?.DEPLOY_USER ? '').split('@')[0]

    comparator: 'name'

module.exports = Requests