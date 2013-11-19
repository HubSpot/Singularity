Collection = require './collection'

class Requests extends Collection

    parse: (requests) ->
        _.each requests, (request, i) =>
            request.id = request.id
            request.name = request.name ? request.id
            request.deployUser = (request.executorData?.env?.DEPLOY_USER ? '').split('@')[0]
            request.JSONString = utils.stringJSON request
            request.timestampHuman = if request?.timestamp? then moment(request.timestamp).from() else ''
            requests[i] = request

        requests

    comparator: 'name'

module.exports = Requests