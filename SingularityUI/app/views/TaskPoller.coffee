HistoricalTasks = require '../collections/HistoricalTasks'
TaskHistoryItem = require '../models/TaskHistoryItem' # To find completed tasks
TaskId = require '../models/TaskId' # To find running tasks
Task = require '../models/Task' # To find files of running tasks

TaskFiles = require '../collections/TaskFiles'

taskPollerWaitingTemplate = require 'templates/vex/taskPollerWaiting'
taskPollingFailureTemplate = require 'templates/vex/taskPollingFailure'

Utils = require '../utils'

vex = require 'vex.dialog'
moment = require 'moment'

interval = (a, b) -> setInterval(b, a)  # f u javascript
timeout = (a, b) -> setTimeout(b, a)

TIMEOUT_MINUTES = 1
TIMEOUT_MILLISECONDS = moment.duration(TIMEOUT_MINUTES, 'minutes').asMilliseconds()

POLLING_TYPES = ['autoTail', 'browse-to-sandbox']


class TaskPoller extends Backbone.View

    initialize: ({@requestId, @autoTailFilename, @taskPollTimestamp, @pollingType, @runId}) ->
        if @pollingType not in POLLING_TYPES
            throw new Error "#{@pollingType} is not a valid polling type for the task poller. Valid polling types are: #{POLLING_TYPES}"

        @possiblyRunningTask = new TaskId {
                requestId: @requestId
                runId: @runId
            }
        @possiblyCompleteTask = new TaskHistoryItem {
                requestId: @requestId
                runId: @runId
            }

        @fetchTasks()


    # Start polling for task changes, and check
    # Task History changes in case we need
    # to back out of the file redirect
    startTaskPolling: ->
        @showTaskPollWaitingDialog()
        @stopTaskPolling()

        @taskPollInterval = interval 2000, =>
            if @pollingType is 'autoTail' and @autoTailTaskFiles
                @autoTailTaskFiles.fetch().error -> app.caughtError()  # we don't care about errors in this situation
            @fetchTasks()

        @taskTimeout = timeout TIMEOUT_MILLISECONDS, =>
            @stopTaskPolling()
            vex.close()
            vex.dialog.alert
                message: taskPollingFailureTemplate
                    autoTailFilename: if @pollingType is 'autoTail' then @autoTailFilename else ''
                    timeout: TIMEOUT_MINUTES
                    problem: 'TIMEOUT'
                buttons: [
                    $.extend _.clone(vex.dialog.buttons.YES), text: 'OK'
                ]

    lastFileCheck: =>
        vex.close()
        if @autoTailTaskFiles and @autoTailTaskFiles.findWhere({name: @autoTailFilename})
            @stopTaskPolling()
            app.router.navigate "#task/#{@taskPollTaskId}/tail/#{@taskPollTaskId}/#{@autoTailFilename}", trigger: true
        else
            @stopTaskPolling()
            vex.dialog.alert
                message: taskPollingFailureTemplate
                    autoTailFilename: @autoTailFilename
                    problem: 'FILE_WONT_EXIST'
                buttons: [
                    $.extend _.clone(vex.dialog.buttons.YES), text: 'OK'
                ]

    taskFound: (task) =>
        timestamp = @taskPollTimestamp
        @taskPollTaskId = if task.id then task.id else task.get('taskId').id
        if @pollingType is 'browse-to-sandbox'
            @browseToSandbox()
        else if @pollingType is 'autoTail'
            unless @notFirstFound
                @notFirstFound = true
                $('.task-poller-checklist').addClass 'waiting-for-file'
                @autoTailTaskFiles = new TaskFiles [], taskId: @taskPollTaskId
            @lastFileCheck() if task is @possiblyCompleteTask

    fetchTasks: =>
        @fetchTask @possiblyRunningTask unless @taskPollTaskId # If we've already found a task we only care if the task is in a terminal state
        @fetchTask @possiblyCompleteTask

    fetchTask: (task) =>
        taskFetch = task.fetch()
        taskFetch.error (error) ->
            Utils.ignore404 error # 404 is expected unless the task is in a terminal state
            # Some browsers will log the http error no matter what, so let the user know that it's ok
            # Not all browsers explicitly say it's a 404, but most say some variant of 'not found'
            console.log "This 'not found' error was expected and may safely be ignored." if error.status is 404
        taskFetch.success () => @taskFound task

    browseToSandbox: =>
        @stopTaskPolling()
        app.router.navigate "#task/#{@taskPollTaskId}", trigger: true
        vex.close()

    handleTaskFilesAdd: =>
        if @pollingType is 'autoTail' and @autoTailTaskFiles.findWhere({name: @autoTailFilename})
            @stopTaskPolling()
            app.router.navigate "#task/#{@taskPollTaskId}/tail/#{@taskPollTaskId}/#{@autoTailFilename}", trigger: true
            vex.close()

    stopTaskPolling: ->
        if @taskPollInterval
            clearInterval @taskPollInterval
        if @taskTimeout
            clearTimeout @taskTimeout
        if @pollingType is 'autoTail'
            if @autoTailTaskFiles
                @stopListening @autoTailTaskFiles, 'add', @handleTaskFilesAdd
            @autoTailTaskFiles = null

    ## Prompt for cancelling the redirect after it's been initiated
    showTaskPollWaitingDialog: ->
        vex.dialog.alert
            overlayClosesOnClick: false
            message: taskPollerWaitingTemplate
                autoTailFilename: @autoTailFilename
            buttons: [
                $.extend _.clone(vex.dialog.buttons.NO), text: 'Close'
            ]
            afterOpen: =>
                unless @pollingType is 'autoTail'
                    $('.wait-for-file-exists').addClass('hide')
            callback: (data) =>
                @stopTaskPolling()


module.exports = TaskPoller
