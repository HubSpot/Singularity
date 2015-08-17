Collection = require './collection'

Task = require '../models/TaskPending'

class TasksPending extends Collection

    model: Task

    url: -> "#{ config.apiRoot }/tasks/scheduled/request/#{ @requestID }"

    initialize: (models = [], { @requestID }) =>

    getTaskByRuntime: (nextRunTime) =>
        for model in @models
            if model.get('pendingTask').pendingTaskId.nextRunAt is nextRunTime
                return model

module.exports = TasksPending