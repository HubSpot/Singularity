View = require './view'

class requestHeaderSubview extends View

    template: require '../templates/requestDetail/requestHeader'

    initialize: ({@model, @taskCleanups, @activeTasks}) ->
        @listenTo @model, 'change', @render
        @listenTo @model, 'sync', @render
        @listenTo @taskCleanups, 'change', @render
        @listenTo @taskCleanups, 'sync', @render
        @listenTo @activeTasks, 'change', @render
        @listenTo @activeTasks, 'sync', @render

    render: =>
        return if not @model.synced
        @$el.html @template @renderData()

    renderData: =>
        bounces = @taskCleanups.where
            cleanupType: 'BOUNCING'
            requestId: @model.get('id')

        deployingInstanceCount = 0

        if !!@model.get('pendingDeploy')
            deployingInstanceCount = @activeTasks.where({deployId: @model.get('pendingDeploy').id, lastTaskState: 'TASK_RUNNING'}).length

        isBouncing: bounces?.length > 0 and @taskCleanups.synced and @activeTasks.synced
        runningInstanceCount: @activeTasks.where({lastTaskState: 'TASK_RUNNING'}).length
        deployingInstanceCount: deployingInstanceCount
        isDeploying: !!@model.get('pendingDeploy') and @model.synced and @activeTasks.synced
        config: config
        data:      @model.toJSON()
        synced:    @model.synced
        canDisableHealthchecks: !!@model.toJSON().activeDeploy and !!@model.toJSON().activeDeploy.healthcheckUri and !@model.toJSON().paused and !@model.toJSON().expiringSkipHealthchecks
        pauseDisabled: !!@model.toJSON().expiringPause and (@model.toJSON().expiringPause.startMillis + @model.toJSON().expiringPause.expiringAPIRequestObject.durationMillis) > new Date().getTime()
        scaleDisabled: !!@model.toJSON().expiringScale and (@model.toJSON().expiringScale.startMillis + @model.toJSON().expiringScale.expiringAPIRequestObject.durationMillis) > new Date().getTime()
        bounceDisabled: !!@model.toJSON().expiringBounce and (@model.toJSON().expiringBounce.startMillis + @model.toJSON().expiringBounce.expiringAPIRequestObject.durationMillis) > new Date().getTime()

module.exports = requestHeaderSubview
