Model = require './model'

class Task extends Model

    url: -> "http://#{env.SINGULARITY_BASE}/#{constants.api_base}/task/#{@get('name')}"

module.exports = Task