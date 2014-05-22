Model = require './model'

class Task extends Model

    url: => "#{ config.apiBase }/tasks/task/#{ @get('id') }"

module.exports = Task