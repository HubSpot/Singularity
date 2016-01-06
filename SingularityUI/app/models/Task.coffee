Model = require './model'
Request = require './Request'

endsWith = (needle, haystack) ->
    position = haystack.length - needle.length
    lastIndex = haystack.indexOf(needle, position);
    return lastIndex isnt -1 && lastIndex is position;

class Task extends Model

    url: => "#{ config.apiRoot }/tasks/task/#{ @get 'id' }"

    initialize: ->
    # Won't be displayed in JSON dialog
    ignoreAttributes: ['id', 'host', 'cpus', 'memoryMb']

    parse: (task) ->
        if task.offer?.hostname
            task.host = task.offer?.hostname

        if window.config.commonHostnameSuffixToOmit.length > 0 and endsWith(window.config.commonHostnameSuffixToOmit, task.host)
            task.host = task.host.substring(0, task.host.length - window.config.commonHostnameSuffixToOmit.length)

        unless task.rackId
            task.rackId = task.taskId?.sanitizedRackId

        if task.mesosTask?
            task.cpus     = _.find(task.mesosTask.resources, (resource) -> resource.name is 'cpus')?.scalar?.value ? ''
            task.memoryMb = _.find(task.mesosTask.resources, (resource) -> resource.name is 'mem')?.scalar?.value ? ''

        if task.taskId?
            task.id = task.taskId.id
        else if task.pendingTask?
            task.id = "#{ task.pendingTask.pendingTaskId.requestId }-#{ task.pendingTask.pendingTaskId.nextRunAt }-#{ task.pendingTask.pendingTaskId.instanceNo }"

        task

    kill: (message, override=false) =>
        params =
            user: app.getUsername()
            override: override

        $.ajax
            url: "#{ @url() }?#{ $.param params }"
            type: "DELETE"
            contentType: 'application/json'
            data: JSON.stringify(
                message: message
            )

    runShellCommand: (cmd, options = []) =>
        params =
            user: app.getUsername()

        data =
            name: cmd
            options: options

        return $.ajax
            url: "#{ @url() }/command?#{ $.param params }"
            type: "POST"
            contentType: 'application/json'
            data: JSON.stringify data

    promptRun: (callback) =>
        # We tell the Request to run
        requestModel = new Request id: @get('request').id
        requestModel.promptRun => callback()



module.exports = Task
