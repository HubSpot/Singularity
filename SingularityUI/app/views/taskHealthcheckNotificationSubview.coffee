View = require './view'
Task = require '../models/Task'

class taskHealthcheckNotificationSubview extends View

    events: ->
        _.extend super,
            'click [data-action="viewHealthchecks"]': 'triggerToggleHealthchecks'

    initialize: ({@model, @template, @pendingDeploys}) ->
        @listenTo @model, 'change', @render
        @listenTo @model, 'sync', @render
        @listenTo @pendingDeploys, 'sync', @render

    render: =>
        return if not @model.synced
        @$el.html @template @renderData()

    renderData: =>
        requestId = @model.get('task').taskId.requestId
        deployId = @model.get('task').taskId.deployId
        deployStatus = @pendingDeploys.find (item) -> item.get('deployMarker') and item.get('deployMarker').requestId is requestId and item.get('deployMarker').deployId is deployId and item.get('currentDeployState') is 'WAITING'

        taskHealthyMessage = "Waiting for successful load balancer update"

        if deployStatus and deployStatus.get('deployProgress')
            if deployStatus.get('deployProgress').stepComplete
                instanceNo = @model.get('task').taskId.instanceNo
                targetInstances = deployStatus.get('deployProgress').targetActiveInstances
                prevTarget = deployStatus.get('deployProgress').targetActiveInstances - deployStatus.get('deployProgress').deployInstanceCountPerStep
                if (instanceNo <= targetInstances and instanceNo > prevTarget) or not @model.get('task').taskRequest.request.loadBalanced
                    taskHealthyMessage = "Waiting for subsequent deploy steps to complete"

        data:             @model.toJSON()
        deployStatus:     deployStatus
        taskHealthyMessage: taskHealthyMessage
        hasSuccessfulHealthcheck: @model.get('healthcheckResults')?.length > 0 and _.find(@model.get('healthcheckResults'), (item) -> item.statusCode is 200)
        lastHealthcheckFailed: @model.get('healthcheckResults')?.length > 0 and @model.get('healthcheckResults')[0].statusCode isnt 200
        synced:           @model.synced
        config:           config

    triggerToggleHealthchecks: ->
        @trigger 'toggleHealthchecks'


module.exports = taskHealthcheckNotificationSubview
