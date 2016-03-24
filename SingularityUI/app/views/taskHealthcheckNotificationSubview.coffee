View = require './view'
Task = require '../models/Task'

class taskHealthcheckNotificationSubview extends View

    noHealthcheckMessageStates: [
        'TASK_FAILED',
        'TASK_FINISHED',
        'TASK_LOST'
    ]

    events: ->
        _.extend super,
            'click [data-action="viewHealthchecks"]': 'triggerToggleHealthchecks'

    initialize: ({@model, @template, @pendingDeploys}) ->
        @listenTo @model, 'change', @render
        @listenTo @model, 'sync', @render
        @listenTo @pendingDeploys, 'sync', @render

    outsideDeployFailureKilledTask: => # True if a deploy failure caused by a different task killed this task
        updates = @model.get('taskUpdates')
        return false unless updates
        thisTaskFailedDeploy = false
        if @deploy and @deploy.attributes.deployResult and @deploy.attributes.deployResult.deployFailures
            @deploy.attributes.deployResult.deployFailures.map (failure) =>
                thisTaskFailedDeploy = true if failure.taskId and failure.taskId.id is @model.attributes.taskId
        return false if thisTaskFailedDeploy
        for update in updates
            return true if update.statusMessage and update.statusMessage.indexOf('DEPLOY_FAILED') isnt -1
        return false

    render: =>
        return if not @model.synced
        return if @model.attributes.lastKnownState in @noHealthcheckMessageStates
        @$el.html @template @renderData()

    renderData: =>
        updates = @model.get('taskUpdates')
        requestId = @model.get('task').taskId.requestId
        deployId = @model.get('task').taskId.deployId
        deployStatus = @pendingDeploys.find (item) -> item.get('deployMarker') and item.get('deployMarker').requestId is requestId and item.get('deployMarker').deployId is deployId and item.get('currentDeployState') is 'WAITING'
        healthTimeoutSeconds = if @model.get('task').taskRequest.deploy.healthcheckMaxTotalTimeoutSeconds then @model.get('task').taskRequest.deploy.healthcheckMaxTotalTimeoutSeconds else config.defaultDeployHealthTimeoutSeconds
        maxRetries = if @model.get('task').taskRequest.deploy.healthcheckMaxRetries then @model.get('task').taskRequest.deploy.healthcheckMaxRetries else config.defaultHealthcheckMaxRetries

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
        healthcheckFailureReasonMessage: @healthcheckFailureReasonMessage()
        synced:           @model.synced
        config:           config
        tooManyRetries: @model.get('healthcheckResults').length > maxRetries and maxRetries != 0
        numberFailed: @model.get('healthcheckResults').length
        secondsElapsed: healthTimeoutSeconds
        doNotDisplayHealthcheckNotification: @outsideDeployFailureKilledTask()

    healthcheckFailureReasonMessage: () -> # For now this only looks for connection refused, but feel free to improve the logic to detect more reasons.
        healthcheckResults = @model.get('healthcheckResults')
        if healthcheckResults and healthcheckResults.length > 0
            if healthcheckResults[0].errorMessage and healthcheckResults[0].errorMessage.toLowerCase().indexOf('connection refused') isnt -1
                portIndex = @model.attributes.task.taskRequest.deploy.healthcheckPortIndex or 0
                port = if @model.attributes.ports.length > portIndex then @model.attributes.ports[portIndex] else false
                return "a refused connection. It is possible your app did not start properly or was not listening on the anticipated port (#{port}). Please check the logs for more details."

    triggerToggleHealthchecks: ->
        @trigger 'toggleHealthchecks'


module.exports = taskHealthcheckNotificationSubview
