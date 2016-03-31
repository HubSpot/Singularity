HistoricalTasks = require '../collections/HistoricalTasks'
TaskHistoryItem = require '../models/TaskHistoryItem'

TaskFiles = require '../collections/TaskFiles'
RequestTasks = require '../collections/RequestTasks'

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

        @task = new TaskHistoryItem {
            requestId: @requestId
            runId: @runId
        }

        @activeTasks = new RequestTasks [],
            requestId: @requestId
            state:    'active'

        @fetchTask()
        @activeTasks.fetch().error    Utils.ignore404


    # Start polling for task changes, and check
    # Task History changes in case we need
    # to back out of the file redirect
    startTaskPolling: ->
        @showTaskPollWaitingDialog()
        @stopTaskPolling()

        @listenTo @activeTasks, 'reset', @handleActiveTasksAdd

        @taskPollInterval = interval 2000, =>
            if @pollingType is 'autoTail' and @autoTailTaskFiles
                @autoTailTaskFiles.fetch().error -> app.caughtError()  # we don't care about errors in this situation
            else
                @activeTasks.fetch()
                @fetchTask()

        @taskTimeout = timeout TIMEOUT_MILLISECONDS, =>
            @stopTaskPolling()
            vex.close()
            vex.dialog.alert
                message: taskPollingFailureTemplate
                    autoTailFilename: if @pollingType is 'autoTail' then @autoTailFilename else ''
                    timeout: TIMEOUT_MINUTES
                buttons: [
                    $.extend _.clone(vex.dialog.buttons.YES), text: 'OK'
                ]

    handleActiveTasksAdd: (tasks) =>
        timestamp = @taskPollTimestamp
        matchingTask = tasks.find (task) -> task.get('taskId').startedAt > timestamp
        if matchingTask
            @taskPollTaskId = matchingTask.get('id')
            if @pollingType is 'browse-to-sandbox'
                @browseToSandbox()
            else if @pollingType is 'autoTail'
                $('.task-poller-checklist').addClass 'waiting-for-file'
                @autoTailTaskFiles = new TaskFiles [], taskId: @taskPollTaskId
                @listenTo @autoTailTaskFiles, 'add', @handleTaskFilesAdd
            @stopListening @activeTasks, 'reset'

    taskFound: =>
        timestamp = @taskPollTimestamp
        @taskPollTaskId = @task.get('taskId').id
        if @pollingType is 'browse-to-sandbox'
            @browseToSandbox()
        else if @pollingType is 'autoTail'
            $('.task-poller-checklist').addClass 'waiting-for-file'
            @autoTailTaskFiles = new TaskFiles [], taskId: @taskPollTaskId

    fetchTask: =>
        taskFetch = @task.fetch()
        taskFetch.error (error) ->
            Utils.ignore404 error # 404 is expected unless the task is in a terminal state
            console.log 'The above 404 was expected and may safely be ignored.' if error.status is 404
        taskFetch.success @taskFound

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
        @stopListening @activeTasks, 'add', @handleActiveTasksAdd
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
