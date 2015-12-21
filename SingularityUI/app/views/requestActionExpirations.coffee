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
        if request.expiringScale and (request.expiringScale.startMillis + request.expiringScale.durationMillis) > new Date().getTime()
            expirations.push
                action: 'Scale'
                endMillis: request.expiringScale.startMillis + request.expiringScale.durationMillis
                canRevert: true
                cancelText: 'Make Permanent'
                cancelAction: "makeScalePermanent"
                revertText: "Revert to #{request.expiringScale.revertToInstances} #{if request.expiringScale.revertToInstances is 1 then 'instance' else 'instances'}"
                revertAction: 'revertScale'
                revertParam: request.expiringScale.revertToInstances
        if request.expiringBounce and (request.expiringBounce.startMillis + request.expiringBounce.durationMillis) > new Date().getTime()
            expirations.push
                action: 'Bounce'
                endMillis: request.expiringBounce.startMillis + request.expiringBounce.durationMillis
                canRevert: false
                cancelText: 'Cancel'
                cancelAction: 'cancelBounce'
        if request.expiringPause and (request.expiringPause.startMillis + request.expiringPause.durationMillis) > new Date().getTime()
            expirations.push
                action: 'Pause'
                endMillis: request.expiringPause.startMillis + request.expiringPause.durationMillis
                canRevert: true
                cancelText: 'Make Permanent'
                cancelAction: 'makePausePermanent'
                revertText: "Unpause"
                revertAction: 'revertPause'

        request: request
        data: expirations

module.exports = requestActionExpirations
