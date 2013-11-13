Model = require './model'

class Request extends Model

    url: -> "http://#{env.SINGULARITY_BASE}/#{constants.api_base}/history/request/#{@get('name')}"

module.exports = Request