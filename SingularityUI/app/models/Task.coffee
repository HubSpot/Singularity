Model = require './model'

class Task extends Model

    url: => "#{ window.singularity.config.apiBase }/tasks/task/#{ @get('id') }"

module.exports = Task