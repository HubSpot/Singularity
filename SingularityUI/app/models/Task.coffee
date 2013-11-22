Model = require './model'

class Task extends Model

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/tasks/task/#{ @get('id') }"

module.exports = Task