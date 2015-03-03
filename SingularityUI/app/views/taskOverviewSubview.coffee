View = require './view'
Task = require '../models/Task'

TasksCleanup = require '../collections/Tasks'



class taskOverviewSubview extends View

    events: ->
        _.extend super,
            'click [data-action="remove"]': 'killTask'


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
        @$el.html @template(@renderData())

    renderData: ->
        data =
            config:    config
            data:      @model.toJSON()
            synced:    @model.synced

        data


    killPrompt: ->
        @taskModel.promptKill @killType, =>
            if @killType is 'kill9'
                @model.set 'killStatus', 'kill9'

            # Poll for 'Cleanup' changes so we can 
            # update kill messages/buttons
            x = 0
            taskKillInterval = setInterval((=>
                @trigger 'refreshrequest'
                @getCleaningStatus()

                if ++x == 4 or  !@model.get 'isStillRunning'
                    clearInterval taskKillInterval 
                return

            ), 1500)


    
    killTask: (event) ->
        @killType = $(event.currentTarget).data('kill-type')      
        @taskModel = new Task id: @model.taskId

        # Check if the status is no longer in Cleanup since clicking the kill button
        if @killType is 'killOverride'
            @getCleaningStatus =>
                @killType = 'kill9Warning' if not @model.get 'isInCleanup'
                @killPrompt()
            return
        
        @killPrompt()







module.exports = taskOverviewSubview