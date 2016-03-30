HistoricalTasks = require '../collections/HistoricalTasks'

TaskFiles = require '../collections/TaskFiles'
RequestTasks = require '../collections/RequestTasks'

taskPollerWaitingTemplate = require 'templates/vex/taskPollerWaiting'
taskPollingFailureTemplate = require 'templates/vex/taskPollingFailure'

vex = require 'vex.dialog'

interval = (a, b) -> setInterval(b, a)  # f u javascript
timeout = (a, b) -> setTimeout(b, a)

TIMEOUT_MINUTES = 1 # Modify this
TIMEOUT_SECONDS = TIMEOUT_MINUTES * 60 # Don't modify this (unless you want a timeout less than a minute)
TIMEOUT_MILLISECONDS = TIMEOUT_SECONDS * 1000 # Don't modify this

POLLING_TYPES = ['autoTail', 'browse-to-sandbox']


class TaskPoller extends Backbone.View

    initialize: ({@requestId, @autoTailFilename, @taskPollTimestamp, @pollingType}) ->

        @history     = new HistoricalTasks [], {params: {requestId: @requestId}}

        @activeTasks = new RequestTasks [],
            requestId: @requestId
            state:    'active'

        @activeTasks.fetch().error    @ignore404

        if @history.currentPage is 1
            @history.fetch().error    @ignore404

        if @pollingType not in POLLING_TYPES
            throw new Error "#{@pollingType} is not a valid polling type for the task poller. Valid polling types are: #{POLLING_TYPES}"


    # Start polling for task changes, and check
    # Task History changes in case we need
    # to back out of the file redirect
    startTaskPolling: ->
        @showTaskPollWaitingDialog()
        @stopTaskPolling()

        @listenTo @history, 'reset', @handleHistoryReset
        @listenTo @activeTasks, 'reset', @handleActiveTasksAdd

        @taskPollInterval = interval 2000, =>
            if @pollingType is 'autoTail' and @autoTailTaskFiles
                @autoTailTaskFiles.fetch().error -> app.caughtError()  # we don't care about errors in this situation
            else
                @history.fetch()
                @activeTasks.fetch()

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

    handleHistoryReset: (tasks) =>
        timestamp = @taskPollTimestamp
        matchingTask = tasks.find (task) -> task.get('taskId').startedAt > timestamp
        if matchingTask
            @taskPollTaskId = matchingTask.get('id')
            if @pollingType is 'browse-to-sandbox'
                @browseToSandbox()
            else if @pollingType is 'autoTail'
                $('.task-poller-checklist').addClass 'waiting-for-file'
                @autoTailTaskFiles = new TaskFiles [], taskId: @taskPollTaskId
            @stopListening @activeTasks, 'reset'

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

    browseToSandbox: =>
        @stopTaskPolling()
        app.router.navigate "#task/#{@taskPollTaskId}", trigger: true
        vex.close()

    handleTaskFilesAdd: =>
        if @pollingType is 'autoTail' and @autoTailTaskFiles.findWhere({name: @autoTailFilename})
            @stopTaskPolling()
            @stopListening @history, 'reset', @handleHistoryReset
            app.router.navigate "#task/#{@taskPollTaskId}/tail/#{@taskPollTaskId}/#{@autoTailFilename}", trigger: true
            vex.close()

    stopTaskPolling: ->
        if @taskPollInterval
            clearInterval @taskPollInterval
        if @taskTimeout
            clearTimeout @taskTimeout
        @stopListening @activeTasks, 'add', @handleActiveTasksAdd
        @stopListening @history, 'reset', @handleHistoryReset
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
