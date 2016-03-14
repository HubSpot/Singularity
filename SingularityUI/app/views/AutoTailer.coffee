HistoricalTasks = require '../collections/HistoricalTasks'

TaskFiles = require '../collections/TaskFiles'
RequestTasks = require '../collections/RequestTasks'

autoTailWaitingTemplate = require 'templates/vex/autoTailingWaiting'
autoTailFailureTemplate = require 'templates/vex/autoTailingFailure'

vex = require 'vex.dialog'

interval = (a, b) -> setInterval(b, a)  # f u javascript
timeout = (a, b) -> setTimeout(b, a)

AUTO_TAIL_TIMEOUT = 5 * 60 * 1000


class AutoTailer extends Backbone.View

    initialize: ({@requestId, @autoTailFilename, @autoTailTimestamp}) ->

        @history     = new HistoricalTasks [], {params: {requestId: @requestId}}

        @activeTasks = new RequestTasks [],
            requestId: @requestId
            state:    'active'

        @activeTasks.fetch().error    @ignore404

        if @history.currentPage is 1
            @history.fetch().error    @ignore404


    # Start polling for task changes, and check
    # Task History changes in case we need
    # to back out of the file redirect
    startAutoTailPolling: ->
        @showAutoTailWaitingDialog()
        @stopAutoTailPolling()

        @listenTo @history, 'reset', @handleHistoryReset
        @listenTo @activeTasks, 'reset', @handleActiveTasksAdd

        @autoTailPollInterval = interval 2000, =>
            if @autoTailTaskFiles
                @autoTailTaskFiles.fetch().error -> app.caughtError()  # we don't care about errors in this situation
            else
                @history.fetch()
                @activeTasks.fetch()

        @autoTailTimeout = timeout 60000, =>
            @stopAutoTailPolling()
            vex.close()
            vex.dialog.alert
                message: autoTailFailureTemplate
                    autoTailFilename: @autoTailFilename
                    timeout: Math.floor(AUTO_TAIL_TIMEOUT / 60000)
                buttons: [
                    $.extend _.clone(vex.dialog.buttons.YES), text: 'OK'
                ]

    handleHistoryReset: (tasks) =>
        timestamp = @autoTailTimestamp
        matchingTask = tasks.find (task) -> task.get('taskId').startedAt > timestamp
        if matchingTask
            $('.auto-tail-checklist').addClass 'waiting-for-file'
            @stopListening @activeTasks, 'reset'
            @autoTailTaskFiles = new TaskFiles [], taskId: matchingTask.get('id')

    handleActiveTasksAdd: (tasks) =>
        timestamp = @autoTailTimestamp
        matchingTask = tasks.find (task) -> task.get('taskId').startedAt > timestamp
        if matchingTask
            $('.auto-tail-checklist').addClass 'waiting-for-file'
            @autoTailTaskId = matchingTask.get('id')
            @autoTailTaskFiles = new TaskFiles [], taskId: @autoTailTaskId
            @stopListening @activeTasks, 'reset'
            @listenTo @autoTailTaskFiles, 'add', @handleTaskFilesAdd

    handleTaskFilesAdd: =>
        if @autoTailTaskFiles.findWhere({name: @autoTailFilename})
            @stopAutoTailPolling()
            @stopListening @history, 'reset', @handleHistoryReset
            app.router.navigate "#task/#{@autoTailTaskId}/tail/#{@autoTailTaskId}/#{@autoTailFilename}", trigger: true
            vex.close()

    stopAutoTailPolling: ->
        if @autoTailPollInterval
            clearInterval @autoTailPollInterval
        if @autoTailTimeout
            clearTimeout @autoTailTimeout
        @stopListening @activeTasks, 'add', @handleActiveTasksAdd
        @stopListening @history, 'reset', @handleHistoryReset
        if @autoTailTaskFiles
            @stopListening @autoTailTaskFiles, 'add', @handleTaskFilesAdd
        @autoTailTaskFiles = null

    ## Prompt for cancelling the redirect after it's been initiated
    showAutoTailWaitingDialog: ->
        vex.dialog.alert
            overlayClosesOnClick: false
            message: autoTailWaitingTemplate
                autoTailFilename: @autoTailFilename
            buttons: [
                $.extend _.clone(vex.dialog.buttons.NO), text: 'Close'
            ]
            callback: (data) =>
                @stopAutoTailPolling()


module.exports = AutoTailer
