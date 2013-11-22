Model = require './model'

class Request extends Model

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/request/#{ @get('id') }"

module.exports = Request