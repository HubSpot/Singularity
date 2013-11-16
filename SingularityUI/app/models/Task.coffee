Model = require './model'

class Task extends Model

    url: -> "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/task/#{ @get('name') }"

module.exports = Task