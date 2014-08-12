Model = require './model'

pauseTemplate = require '../templates/vex/requestPause'
unpauseTemplate = require '../templates/vex/requestUnpause'
runTemplate = require '../templates/vex/requestRun'
removeTemplate = require '../templates/vex/requestRemove'
bounceTemplate = require '../templates/vex/requestBounce'

class Request extends Model

    # When we show the JSON dialog, we will ignore these attributes
    ignoreAttributes: ['id', 'scheduled', 'onDemand', 'daemon', 'paused', 'deleted', 'hasActiveDeploy', 'canBeRunNow', 'canBeBounced', 'starred']

    url: => "#{ config.apiRoot }/requests/request/#{ @get('id') }"

    parse: (data) ->
        if data.deployId?
            # For pending tasks
            data.id = data.deployId
            return data
        else
            data.id = data.request.id

        # Gotta fecking figure out what kind of request this is
        data.scheduled = typeof data.request.schedule is 'string'
        data.onDemand = data.request.daemon? and not data.request.daemon and not data.scheduled
        data.daemon = not data.scheduled and not data.onDemand

        data.paused = data.state is 'PAUSED'
        data.deleted = data.state is 'DELETED'

        data.hasActiveDeploy = data.activeDeploy? or data.requestDeployState?.activeDeploy?
        data.canBeRunNow = data.state is 'ACTIVE' and (data.scheduled or data.onDemand) and data.hasActiveDeploy
        data.canBeBounced = data.state in ['ACTIVE', 'SYSTEM_COOLDOWN']

        data

    deletePaused: =>
        $.ajax
            url:  "#{ @url() }/paused"
            type: 'DELETE'

    unpause: =>
        $.ajax
            url:  "#{ @url() }/unpause?user=#{ app.getUsername() }"
            type: 'POST'

    pause: (killTasks) =>
        killTasks = if not killTasks then undefined else killTasks

        $.ajax
            url:         "#{ @url() }/pause"
            type:        'POST'
            contentType: 'application/json'
            data:         JSON.stringify
                user:      app.getUsername()
                killTasks: killTasks

    run: (confirmedOrPromptData) ->
        options =
            url: "#{ @url() }/run?user=#{ app.getUsername() }"
            type: 'POST'
            contentType: 'application/json'

        if typeof confirmedOrPromptData is 'string'
            options.data = confirmedOrPromptData
            options.processData = false
            options.contentType = 'text/plain'

        $.ajax options
        
    bounce: =>
        $.ajax
            url:  "#{ @url() }/bounce?user=#{ app.getUsername() }"
            type: "POST"

    destroy: =>
        $.ajax
            url:  "#{ @url() }?user=#{ app.getUsername() }"
            type: "DELETE"

    ###
    promptX opens a dialog asking the user to confirm an action and then does it
    ###
    promptPause: (callback) =>
        vex.dialog.confirm
            message: pauseTemplate
                id:        @get 'id'
                scheduled: @get 'scheduled'
            callback: (confirmed) =>
                return unless confirmed
                killTasks = $('.vex #kill-tasks').is ':checked'
                @pause(killTasks).done callback

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
                $.extend _.clone(vex.dialog.buttons.YES), text: 'Run now'
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