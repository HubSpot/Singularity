Model = require './model'

pauseTemplate = require '../views/templates/vex/requestPause'
unpauseTemplate = require '../views/templates/vex/requestUnpause'
runTemplate = require '../views/templates/vex/requestRun'
removeTemplate = require '../views/templates/vex/requestRemove'
bounceTemplate = require '../views/templates/vex/requestBounce'

class Request extends Model
            
    parse: (data) ->
        if data.request?
            data.request.daemon = if _.isNull(data.request.daemon) then true else data.request.daemon
            data.daemon = data.request.daemon
            
            data.scheduled = utils.isScheduledRequest data.request
            data.onDemand = utils.isOnDemandRequest data.request

            data.paused = data.state is 'PAUSED'
            data.deleted = data.state is 'DELETED'
            data.canBeRunNow = (data.scheduled or data.onDemand) and not data.daemon and data.activeDeploy?
            data.canBeBounced = data.state in ['ACTIVE', 'SYSTEM_COOLDOWN']

            data.displayState = constants.requestStates[data.state]

            data.activeDeploy?.timestampHuman = utils.humanTime data.activeDeploy.timestamp

        data

    url: => "#{ config.apiRoot }/requests/request/#{ @get('id') }"

    deletePaused: =>
        $.ajax
            url: "#{ @url() }/paused"
            type: 'DELETE'

    unpause: =>
        $.ajax
            url: "#{ @url() }/unpause?user=#{ app.getUsername() }"
            type: 'POST'

    pause: =>
        $.ajax
            url: "#{ @url() }/pause?user=#{ app.getUsername() }"
            type: 'POST'

    run: (confirmedOrPromptData) ->
        options =
            url: "#{ @url() }/run?user=#{ app.getUsername() }"
            type: 'POST'
            contentType: 'application/json'

        if _.isString confirmedOrPromptData
            options.data = confirmedOrPromptData
            options.processData = false
            options.contentType = 'text/plain'

        $.ajax options
        
    bounce: =>
        $.ajax
            url: "#{ @url() }/bounce?user=#{app.getUsername()}"
            type: "POST"

    destroy: =>
        $.ajax
            url: "#{ @url() }?user=#{app.getUsername()}"
            type: "DELETE"

    ###
    promptX opens a dialog asking the user to confirm an action and then does it
    ###
    promptPause: (callback) =>
        vex.dialog.confirm
            message: pauseTemplate id: @get "id"
            callback: (confirmed) =>
                return unless confirmed
                @pause().done callback

    promptUnpause: (callback) =>
        vex.dialog.confirm
            message: unpauseTemplate id: @get "id"
            callback: (confirmed) =>
                return unless confirmed
                @unpause().done callback

    promptRun: (callback) =>
        vex.dialog.prompt
            message: runTemplate id: @get "id"
            buttons: [
                $.extend vex.dialog.buttons.YES, text: 'Run now'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                @run(data).done callback

    promptRemove: (callback) =>
        vex.dialog.confirm
            message: removeTemplate id: @get "id"
            callback: (confirmed) =>
                return if not confirmed
                @destroy().done callback

    promptBounce: (callback) =>
        vex.dialog.confirm
            message: bounceTemplate id: @get "id"
            callback: (confirmed) =>
                return if not confirmed
                @bounce().done callback


module.exports = Request