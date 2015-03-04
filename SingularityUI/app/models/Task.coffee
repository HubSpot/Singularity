Model = require './model'
Request = require './Request'

class Task extends Model

    url: => "#{ config.apiRoot }/tasks/task/#{ @get 'id' }"

    initialize: ->
    # Won't be displayed in JSON dialog
    ignoreAttributes: ['id', 'host', 'cpus', 'memoryMb']

    parse: (task) ->
        if task.offer?
            task.host = task.offer?.hostname?.split('.')[0]

        if task.mesosTask?
            task.cpus     = _.find(task.mesosTask.resources, (resource) -> resource.name is 'cpus')?.scalar?.value ? ''
            task.memoryMb = _.find(task.mesosTask.resources, (resource) -> resource.name is 'mem')?.scalar?.value ? ''

        if task.taskId?
            task.id = task.taskId.id
        else if task.pendingTask?
            task.id = "#{ task.pendingTask.pendingTaskId.requestId }-#{ task.pendingTask.pendingTaskId.nextRunAt }-#{ task.pendingTask.pendingTaskId.instanceNo }"
        task

    ###
    promptX opens a dialog asking the user to confirm an action and then does it
    ###



    killTask: (type) =>
        username = app.getUsername()
        params =
            user: username

        if type is 'killOverride' or type is 'kill9'
            params.override = true
        
        url = @url() + "?" + $.param params

        $.ajax
            url: url
            type: "DELETE"


    promptRun: (callback) =>
        # We tell the Request to run
        requestModel = new Request id: @get('request').id
        requestModel.promptRun => callback()



module.exports = Task
