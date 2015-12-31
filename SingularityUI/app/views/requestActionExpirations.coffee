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
                action: "Scale (to #{request.request.instances} instances)"
                user: request.expiringScale.user.split('@')[0]
                endMillis: request.expiringScale.startMillis + request.expiringScale.expiringAPIRequestObject.durationMillis
                canRevert: true
                cancelText: 'Make Permanent'
                cancelAction: "makeScalePermanent"
                revertText: "Revert to #{request.expiringScale.revertToInstances} #{if request.expiringScale.revertToInstances is 1 then 'instance' else 'instances'}"
                revertAction: 'revertScale'
                revertParam: request.expiringScale.revertToInstances
                message: request.expiringScale.expiringAPIRequestObject.message

        if request.expiringBounce and (request.expiringBounce.startMillis + request.expiringBounce.expiringAPIRequestObject.durationMillis) > new Date().getTime()
            expirations.push
                action: 'Bounce'
                user: request.expiringBounce.user.split('@')[0]
                endMillis: request.expiringBounce.startMillis + request.expiringBounce.expiringAPIRequestObject.durationMillis
                canRevert: false
                cancelText: 'Cancel'
                cancelAction: 'cancelBounce'
                message: request.expiringBounce.expiringAPIRequestObject.message

        if request.expiringPause and (request.expiringPause.startMillis + request.expiringPause.expiringAPIRequestObject.durationMillis) > new Date().getTime()
            expirations.push
                action: 'Pause'
                user: request.expiringPause.user.split('@')[0]
                endMillis: request.expiringPause.startMillis + request.expiringPause.expiringAPIRequestObject.durationMillis
                canRevert: true
                cancelText: 'Make Permanent'
                cancelAction: 'makePausePermanent'
                revertText: "Unpause"
                revertAction: 'revertPause'
                message: request.expiringPause.expiringAPIRequestObject.message

        if request.expiringSkipHealthchecks and (request.expiringSkipHealthchecks.startMillis + request.expiringSkipHealthchecks.expiringAPIRequestObject.durationMillis) > new Date().getTime()
            expirations.push
                action: if request.expiringSkipHealthchecks.expiringAPIRequestObject.skipHealthchecks then 'Disable Healthchecks' else 'Enable Healthchecks'
                user: request.expiringSkipHealthchecks.user.split('@')[0]
                endMillis: request.expiringSkipHealthchecks.startMillis + request.expiringSkipHealthchecks.expiringAPIRequestObject.durationMillis
                canRevert: true
                cancelText: 'Make Permanent'
                cancelAction: 'makeSkipHealthchecksPermanent'
                revertText: if request.expiringSkipHealthchecks.expiringAPIRequestObject.skipHealthchecks then 'Enable Healthchecks' else 'Disable Healthchecks'
                revertAction: 'revertSkipHealthchecks'
                revertParam: !request.expiringSkipHealthchecks.expiringAPIRequestObject.skipHealthchecks
                message: request.expiringSkipHealthchecks.expiringAPIRequestObject.message

        request: request
        data: expirations

module.exports = requestActionExpirations
