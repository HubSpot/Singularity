View = require './view'

class requestActionExpirations extends View

    template: require '../templates/requestDetail/requestActionExpirations'

    initialize: ({@model}) ->
        @listenTo @model, 'change', @render
        @listenTo @model, 'sync', @render

    render: =>
        return if not @model.synced
        @$el.html @template @renderData()

    renderData: =>
        expirations = []
        request = @model.toJSON()
        if request.expiringScale and (request.expiringScale.startMillis + request.expiringScale.expiringAPIRequestObject.durationMillis) > new Date().getTime()
            expirations.push
                action: 'Scale'
                endMillis: request.expiringScale.startMillis + request.expiringScale.expiringAPIRequestObject.durationMillis
                canRevert: true
                cancelText: 'Make Permanent'
                cancelAction: "makeScalePermanent"
                revertText: "Revert to #{request.expiringScale.revertToInstances} #{if request.expiringScale.revertToInstances is 1 then 'instance' else 'instances'}"
                revertAction: 'revertScale'
                revertParam: request.expiringScale.revertToInstances
        if request.expiringBounce and (request.expiringBounce.startMillis + request.expiringBounce.expiringAPIRequestObject.durationMillis) > new Date().getTime()
            expirations.push
                action: 'Bounce'
                endMillis: request.expiringBounce.startMillis + request.expiringBounce.expiringAPIRequestObject.durationMillis
                canRevert: false
                cancelText: 'Cancel'
                cancelAction: 'cancelBounce'
        if request.expiringPause and (request.expiringPause.startMillis + request.expiringPause.expiringAPIRequestObject.durationMillis) > new Date().getTime()
            expirations.push
                action: 'Pause'
                endMillis: request.expiringPause.startMillis + request.expiringPause.expiringAPIRequestObject.durationMillis
                canRevert: true
                cancelText: 'Make Permanent'
                cancelAction: 'makePausePermanent'
                revertText: "Unpause"
                revertAction: 'revertPause'
        if request.expiringSkipHealthchecks and (request.expiringSkipHealthchecks.startMillis + request.expiringSkipHealthchecks.expiringAPIRequestObject.durationMillis) > new Date().getTime()
            expirations.push
                action: request.expiringSkipHealthchecks.skipHealthchecks ? 'Disable Healthchecks' : 'Enable Healthchecks'
                endMillis: request.expiringSkipHealthchecks.startMillis + request.expiringSkipHealthchecks.expiringAPIRequestObject.durationMillis
                canRevert: true
                cancelText: 'Make Permanent'
                cancelAction: 'makeSkipHealthchecksPermanent'
                revertText: request.expiringSkipHealthchecks.skipHealthchecks ? 'Enable Healthchecks' : 'Disable Healthchecks'
                revertAction: 'revertSkipHealthchecks'
                revertParam: !request.expiringSkipHealthchecks.skipHealthchecks

        request: request
        data: expirations

module.exports = requestActionExpirations
