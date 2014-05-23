Model = require './model'

class Task extends Model

    url: => "#{ config.apiRoot }/tasks/task/#{ @get('id') }"

module.exports = Task