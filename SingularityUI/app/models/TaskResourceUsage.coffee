Model = require './model'

class TaskResourceUsage extends Model

    url: => "#{ config.apiRoot }/tasks/task/#{ @taskId }/statistics"

    initialize: ({ @taskId }) =>

module.exports = TaskResourceUsage
