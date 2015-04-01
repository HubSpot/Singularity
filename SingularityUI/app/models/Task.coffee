Model = require './model'

Request = require './Request'

killTemplate = require '../templates/vex/taskKill'

class Task extends Model

    url: => "#{ config.apiRoot }/tasks/task/#{ @get 'id' }"

    # Won't be displayed in JSON dialog
    ignoreAttributes: ['id', 'host', 'cpus', 'memoryMb']

    parse: (task) ->
        if task.offer?
            task.host = task.offer?.hostname?.split('.')[0]
        else
            task.host = task.host?.split('.')[0]

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

    killTask: =>
        $.ajax
            url: "#{ @url() }?user=#{ app.getUsername() }"
            type: "DELETE"

    promptRun: (callback) =>
        # We tell the Request to run
        requestModel = new Request id: @get('request').id
        requestModel.promptRun => callback()

    promptKill: (callback) =>
        vex.dialog.confirm
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Kill task'
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]

            message: killTemplate id: @get('id')
            callback: (confirmed) =>
                return unless confirmed
                deleteRequest = @killTask()
                deleteRequest.done callback

                # ignore errors (probably means you tried
                # to kill an already dead task)
                deleteRequest.error =>
                    app.caughtError()
                    callback()

module.exports = Task
