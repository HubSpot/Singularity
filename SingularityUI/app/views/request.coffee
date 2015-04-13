View = require './view'

Deploy = require '../models/Deploy'
TaskFiles = require '../collections/TaskFiles'

interval = (a, b) -> setInterval(b, a)  # f u javascript
timeout = (a, b) -> setTimeout(b, a)

AUTO_TAIL_TIMEOUT = 5 * 60 * 1000

class RequestView extends View

    template: require '../templates/requestDetail/requestBase'

    events: ->
        _.extend super,
            'click [data-action="viewJSON"]': 'viewJson'
            'click [data-action="viewObjectJSON"]': 'viewObjectJson'

            'click [data-action="remove"]': 'removeRequest'
            'click [data-action="run-request-now"]': 'runRequest'
            'click [data-action="pause"]': 'pauseRequest'
            'click [data-action="scale"]': 'scaleRequest'
            'click [data-action="unpause"]': 'unpauseRequest'
            'click [data-action="bounce"]': 'bounceRequest'

            'click [data-action="run-now"]': 'runTask'

            'click [data-action="expand-deploy-history"]': 'flashDeployHistory'

    initialize: ({@requestId, @history, @activeTasks}) ->

    render: ->
        @$el.html @template
            config: config

        # Attach subview elements
        @$('#header').html           @subviews.header.$el
        @$('#stats').html            @subviews.stats.$el
        @$('#active-tasks').html     @subviews.activeTasks.$el
        @$('#scheduled-tasks').html  @subviews.scheduledTasks.$el
        @$('#task-history').html     @subviews.taskHistory.$el
        @$('#deploy-history').html   @subviews.deployHistory.$el
        @$('#request-history').html  @subviews.requestHistory.$el

    viewJson: (e) =>
        $target = $(e.currentTarget).parents 'tr'
        id = $target.data 'id'
        collectionName = $target.data 'collection'

        if collectionName is 'deployHistory'
            deploy = new Deploy {},
                requestId: @model.id
                deployId:  id

            utils.viewJSON deploy
        else
            # Need to reach into subviews to get the necessary data
            collection = @subviews[collectionName].collection
            utils.viewJSON collection.get id

    viewObjectJson: (e) =>
        utils.viewJSON @model

    removeRequest: (e) =>
        @model.promptRemove =>
            app.router.navigate 'requests', trigger: true

    runRequest: (e) =>
        @model.promptRun (data) =>   
            # If user wants to redirect to a file after the task starts
            if data.autoTail is 'on'
                @autoTailFilename = data.filename
                @autoTailTimestamp = +new Date()
                @startAutoTailPolling()
            else
                @trigger 'refreshrequest'
                setTimeout ( => @trigger 'refreshrequest'), 2500

    scaleRequest: (e) =>
        @model.promptScale =>
            @trigger 'refreshrequest'

    pauseRequest: (e) =>
        @model.promptPause =>
            @trigger 'refreshrequest'

    unpauseRequest: (e) =>
        @model.promptUnpause =>
            @trigger 'refreshrequest'

    bounceRequest: (e) =>
        @model.promptBounce =>
            @trigger 'refreshrequest'

    runTask: (e) =>
        id = $(e.target).parents('tr').data 'id'

        @model.promptRun =>
            @subviews.scheduledTasks.collection.remove id
            @subviews.scheduledTasks.render()            
            setTimeout =>
                @trigger 'refreshrequest'
            , 3000

    flashDeployHistory: ->
        @subviews.deployHistory.flash()

    # Start polling for task changes, and check
    # Task History changes in case we need 
    # to back out of the file redirect 
    startAutoTailPolling: ->
        @showAutoTailWaitingDialog()
        @stopAutoTailPolling()

        @listenTo @history, 'reset', @handleHistoryReset
        @listenToOnce @activeTasks, 'add', @handleActiveTasksAdd

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
                message: """
                    <h3>Failure</h3>
                    <code>#{ @autoTailFilename }</code> did not exist after #{ Math.floor(AUTO_TAIL_TIMEOUT / 60000) } minute(s).
                """
                buttons: [
                    $.extend _.clone(vex.dialog.buttons.YES), text: 'OK'
                ]

    handleHistoryReset: (tasks) =>
        timestamp = @autoTailTimestamp
        matchingTask = tasks.find (task) -> task.get('taskId').startedAt > timestamp
        if matchingTask
            $('.auto-tail-checklist').addClass 'waiting-for-file'
            @stopListening @activeTasks, 'add'
            @autoTailTaskFiles = new TaskFiles [], taskId: matchingTask.get('id')

    handleActiveTasksAdd: (task) =>
        $('.auto-tail-checklist').addClass 'waiting-for-file'
        @autoTailTaskId = task.get('id')
        @autoTailTaskFiles = new TaskFiles [], taskId: @autoTailTaskId
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
            message: """
                <h3>Launching</h3>
                <ol class="auto-tail-checklist">
                    <li class="auto-tail-task-start">Waiting for task to launch</li>
                    <li class="auto-tail-file-exists">Waiting for <code>#{ @autoTailFilename }</code> to exist</li>
                </ol>
                <div class='page-loader centered cushy'></div>
            """
            buttons: [
                $.extend _.clone(vex.dialog.buttons.NO), text: 'Close'
            ]
            callback: (data) =>
                @stopAutoTailPolling()


module.exports = RequestView
