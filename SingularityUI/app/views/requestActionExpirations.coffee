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
        if request.expiringScale
            expirations.push
                action: 'Scale'
                endMillis: request.expiringScale.startMillis + request.expiringScale.durationMillis
                canRevert: true
                revertAction: "makeScalePermanent"
                revertText: "Revert to #{request.expiringScale.revertToInstances} #{if request.expiringScale.revertToInstances is 1 then 'instance' else 'instances'}"
        if request.expiringBounce
            expirations.push
                action: 'Bounce'
                endMillis: request.expiringScale.startMillis + request.expiringScale.durationMillis
                canRevert: false
        if request.expiringPause
            expirations.push
                action: 'Pause'
                endMillis: request.expiringPause.startMillis + request.expiringPause.durationMillis
                canRevert: false
                cancelText: 'Make Permanent'
                cancelAction: "makePausePermanent"
        console.log expirations

        request: request
        data: expirations

module.exports = requestActionExpirations
