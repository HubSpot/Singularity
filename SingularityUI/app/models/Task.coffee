Model = require './model'

Request = require './Request'

killTemplate = require '../templates/vex/taskKill'
killOverrideTemplate = require '../templates/vex/taskKillOverride'
killDestroyTemplate = require '../templates/vex/taskKillDestroy'
killDestroyWarningTemplate = require '../templates/vex/taskKillDestroyWarning'

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

        if type is 'killOverride' or 'kill9'
            params.override = true
        
        url = @url() + "?" + $.param params

        $.ajax
            url: url
            type: "DELETE"


    promptRun: (callback) =>
        # We tell the Request to run
        requestModel = new Request id: @get('request').id
        requestModel.promptRun => callback()

    # Choose prompt based on if we plan to 
    # gracefully kill (sigterm),s or force kill (kill-9)
    promptKill: (type, callback) =>        
        if type is 'killOverride'
            btnText = 'Override'
            templ = killOverrideTemplate
        else if type is 'kill9'
            btnText = 'Destroy task'
            templ = killDestroyTemplate
        # Warn user if they attempt to gracefully kill a task 
        # but as they kill, that task is no longer in Cleanup
        else if type is 'kill9Warning'
            btnText = 'Destroy task'
            templ = killDestroyWarningTemplate
        else
            btnText = 'Kill task'
            templ = killTemplate

        vex.dialog.confirm
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: btnText
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]

            message: templ id: @get('id')
            callback: (confirmed) =>
                return unless confirmed
                deleteRequest = @killTask(type)
                deleteRequest.done callback

                # ignore errors (probably means you tried
                # to kill an already dead task)
                deleteRequest.error =>
                    app.caughtError()
                    callback()


module.exports = Task
