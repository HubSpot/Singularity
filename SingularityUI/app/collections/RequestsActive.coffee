Collection = require './collection'

Requests = require './Requests'

class RequestsActive extends Requests

    url: "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/requests"

module.exports = RequestsActive