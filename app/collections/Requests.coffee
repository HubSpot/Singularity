Collection = require './collection'

class Requests extends Collection

    url: "http://#{env.SINGULARITY_BASE}/#{constants.api_base}/requests"

    parse: (requests) ->
        _.each requests, (request, i) =>
            request.id = request.name
            request.label = @parseLabelFromName request.name
            request.deployUser = @parseDeployUser request
            request.JSONString = utils.stringJSON request
            requests[i] = request

        requests

    parseLabelFromName: (name) ->
        name.split(':')[0]

    parseDeployUser: (request) ->
        (request.executorData?.env?.DEPLOY_USER ? '').split('@')[0]

    comparator: 'name'

module.exports = Requests