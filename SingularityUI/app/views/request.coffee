View = require './view'

Deploy = require '../models/Deploy'

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
            if data.filename.length > 1
                @listenToOnce @activeTasks, 'add', @redirectToFile
                
                @redirectFilename = data.filename
                @mostRecentTask = @history.first().get('id')

                @startPollingTasks()
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
    startPollingTasks: ->
        @redirectCancelPrompt()
        @activeInterval = setInterval ( => 
            @trigger 'refreshrequest' 
            @checkTaskHistoryChange()
        ), 2000

    stopPollingTasks: ->
        clearInterval @activeInterval
        @stopListening @activeTasks, 'add', @redirectToFile

    # While waiting for the task to start up, if the Task History
    # updates, and the most recent task has a "dead state",
    # it likely never ran, so let's close the prompt and stop polling
    checkTaskHistoryChange: ->
        deadStates = ['TASK_FAILED', 'TASK_LOST', 'TASK_LOST_WHILE_DOWN'] ## include 'TASK_KILLED' ?
        currentTask = @history.first()
        if currentTask.get('id') isnt @mostRecentTask and (currentTask.get('lastTaskState') in deadStates)
            vex.close()
            @stopPollingTasks()
        
    # If redirecting after the task starts,
    # get the id to generate the url
    redirectToFile: (type) ->
        @stopPollingTasks()
        id = @activeTasks.first().get('id')

        ## Give some time for the file to be created
        setTimeout =>
            app.router.navigate "#task/#{id}/tail/#{id}/#{@redirectFilename}", trigger: true
            vex.close()
        , 2000

    ## Prompt for cancelling the redirect after it's been initiated
    redirectCancelPrompt: ->
        vex.dialog.alert
            message: """
                <div class="page-loader" style='display: inline-block'></div>
                Redirecting to <span class='label label-default'>#{@redirectFilename}</span> once task has started.
            """
            buttons: [
                $.extend _.clone(vex.dialog.buttons.YES), text: 'Cancel'
            ]
            callback: (data) =>
                @stopPollingTasks() if data is true


module.exports = RequestView
