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

        data:             @model.toJSON()
        isDeployPending:  !!deployStatus
        hasSuccessfulHealthcheck: @model.get('healthcheckResults')?.length > 0 and _.find(@model.get('healthcheckResults'), (item) -> item.statusCode is 200)
        lastHealthcheckFailed: @model.get('healthcheckResults')?.length > 0 and @model.get('healthcheckResults')[0].statusCode isnt 200
        synced:           @model.synced
        config:           config

    triggerToggleHealthchecks: ->
        @trigger 'toggleHealthchecks'


module.exports = taskHealthcheckNotificationSubview
