View = require './view'
Task = require '../models/Task'

TasksCleanup = require '../collections/Tasks'

killTemplate = require '../templates/vex/taskKill'
killOverrideTemplate = require '../templates/vex/taskKillOverride'
killDestroyTemplate = require '../templates/vex/taskKillDestroy'
killDestroyWarningTemplate = require '../templates/vex/taskKillDestroyWarning'


class taskOverviewSubview extends View

    events: ->
        _.extend super,
            'click [data-action="remove"]': 'killRequest'


    # initialize: ({@collection, @model, @template}) ->
    initialize: ({@model, @template}) ->

        @collection = new TasksCleanup [], state: 'cleaning'

        for eventName in ['sync', 'add', 'remove', 'change']
            @listenTo @model, eventName, @render
            @listenTo @collection, eventName, @render

        @listenTo @model, 'reset', =>
            @$el.empty()

        @getCleaningStatus()
    
    # Store 'Cleanup' state
    getCleaningStatus: (callback) ->   
        @collection.fetch({reset: true}).done =>
            cleanup = _.findWhere @collection.models, id: @model.get 'taskId'
            if cleanup then state = true else state = false
            @model.set 'isInCleanup', state
            callback?()

    render: ->
        return if not @model.synced and @model.isEmpty?()
        @$el.html @template @renderData()

    renderData: ->
        data =
            config:    config
            data:      @model.toJSON()
            synced:    @model.synced

        data

    killRequest: (event) ->
        @killType = $(event.currentTarget).data('kill-type')      
        @taskModel = new Task id: @model.taskId

        # Check if the status is no longer in Cleanup since clicking the kill button
        if @killType is 'killOverride'
            @getCleaningStatus =>
                @killType = 'kill9Warning' if not @model.get 'isInCleanup'
                @promptKill()
            return
        
        @promptKill()


    updateKillStatus: =>
            if @killType is 'kill9' or @killType is 'kill9Warning'
                @model.set 'killStatus', 'kill9'

            @trigger 'refreshrequest'
            @getCleaningStatus()

            # Poll for 'Cleanup' changes so we can update kill messages/buttons
            x = 0
            taskKillInterval = setInterval((=>
                @trigger 'refreshrequest'
                @getCleaningStatus()

                if ++x == 4 or !@model.get 'isStillRunning'
                    clearInterval taskKillInterval 
                return

            ), 1500)


    # Choose prompt based on if we plan to 
    # gracefully kill (sigterm), or force kill (kill-9)
    promptKill: =>
        if @killType is 'killOverride'
            btnText = 'Override'
            templ = killOverrideTemplate
        else if @killType is 'kill9'
            btnText = 'Destroy task'
            templ = killDestroyTemplate
        else if @killType is 'kill9Warning'
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
            message: templ id: @model.taskId
            
            callback: (confirmed) =>
                return unless confirmed

                # check once again if task is no longer in Cleanup
                if @killType is 'killOverride'
                    @getCleaningStatus =>
                        if not @model.get 'isInCleanup'
                            @killType = 'kill9Warning'              
                            @promptKill()
                            return
                        @killTask()

                else
                    @killTask()
                



    killTask: ->
        deleteRequest = @taskModel.killTask @killType
        deleteRequest.done @updateKillStatus()

        # ignore errors (probably means you tried
        # to kill an already dead task)
        deleteRequest.error =>
            app.caughtError()
            @updateKillStatus()


module.exports = taskOverviewSubview