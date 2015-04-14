View = require './view'
Task = require '../models/Task'

class taskHealthcheckNotificationSubview extends View

    events: ->
        _.extend super,
            'click [data-action="viewHealthchecks"]': 'triggerToggleHealthchecks'

    initialize: ({@collection, @model, @template}) ->

        @taskModel = new Task id: @model.taskId
        @shouldPollHealthchecks = true

        for eventName in ['sync', 'add', 'remove', 'change']
            @listenTo @model, eventName, @render
            @listenTo @collection, eventName, @render

        @listenTo @model, 'reset', =>
            @$el.empty()

    render: ->
        return if not @model.synced or not @collection.synced
        @checkForPendingDeploy() if not @isDeployPending and @shouldPollHealthchecks
        @$el.html @template @renderData()

    renderData: ->
        data:             @model.toJSON()
        isDeployPending:  @isDeployPending
        healthCheck:      @model.get('healthcheckResults')[0]
        synced:           @model.synced and @collection.synced

    # If we have a deploy pending for this task,
    # we start polling until we get a healthcheck to show
    checkForPendingDeploy: ->
        @isDeployPending = false
        for deploy in @collection.toJSON()
            if deploy.deployMarker.deployId is @model.get('task').taskId.deployId
                @isDeployPending = true
                @pollForHealthchecks()
                break

    # Poll for Healthchecks if the task 
    # is part of a pending deploy
    pollForHealthchecks: =>
        do fetchModel = =>
            @model.fetch().done =>
                if @model.get('healthcheckResults').length > 0
                    @render()
                    return @shouldPollHealthchecks = false
                    
                setTimeout ( => fetchModel() ), 1500
                   
    triggerToggleHealthchecks: ->
        @trigger 'toggleHealthchecks'


module.exports = taskHealthcheckNotificationSubview