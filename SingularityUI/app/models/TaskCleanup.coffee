Model = require './model'

CLEANUP_MESSAGE_OVERRIDES =
    USER_REQUESTED: (cleanup) -> "Task is being shut down due to being manually killed by #{ cleanup.user ? '(anonymous)'}."

CLEANUP_IS_IMMEDIATE =
    USER_REQUESTED: true

class TaskCleanup extends Model

    url: => "#{ config.apiRoot }/tasks/task/#{ @get('id') }"

    initialize: ->

    parse: (cleanup) ->
        if CLEANUP_MESSAGE_OVERRIDES[cleanup.cleanupType]
            cleanup.message = CLEANUP_MESSAGE_OVERRIDES[cleanup.cleanupType](cleanup)

        cleanup.isImmediate = CLEANUP_IS_IMMEDIATE[cleanup.cleanupType] ? false

        cleanup.id = "#{ cleanup.taskId.id }-#{ cleanup.timestamp }"

        cleanup.requestId = cleanup.taskId?.requestId

        cleanup

module.exports = TaskCleanup
