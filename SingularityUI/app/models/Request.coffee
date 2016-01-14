Model = require './model'

pauseTemplate = require '../templates/vex/requestPause'
scaleTemplate = require '../templates/vex/requestScale'
unpauseTemplate = require '../templates/vex/requestUnpause'
runTemplate = require '../templates/vex/requestRun'
removeTemplate = require '../templates/vex/requestRemove'
bounceTemplate = require '../templates/vex/requestBounce'
exitCooldownTemplate = require '../templates/vex/exitCooldown'
stepDeployTemplate = require '../templates/vex/stepDeploy'
cancelDeployTemplate = require '../templates/vex/cancelDeploy'
TaskHistory = require '../models/TaskHistory'

class Request extends Model

    ## toggle between creating additional properties during parse
    raw: false

    # When we show the JSON dialog, we will ignore these attributes
    ignoreAttributes: ['id', 'paused', 'deleted', 'hasActiveDeploy', 'canBeRunNow', 'canBeBounced', 'starred']
    localStorageCommandLineInputKeyPrefix: 'runRequestCommandLineInput::'

    url: => "#{ config.apiRoot }/requests/request/#{ @get('id') }"

    parse: (data) ->
        if data.deployId?
            # For pending tasks
            data.id = data.deployId
            return data
        else
            data.id = data.request.id

        return data if @raw

        data.type = data.request.requestType

        data.instances = data.request.instances or 1
        data.hasMoreThanOneInstance = data.instances > 1

        data.bounceAfterScale = data.request.bounceAfterScale

        data.paused = data.state is 'PAUSED'
        data.deleted = data.state is 'DELETED'
        data.inCooldown = data.state is 'SYSTEM_COOLDOWN'

        data.hasActiveDeploy = data.activeDeploy? or data.requestDeployState?.activeDeploy?
        data.daemon = data.type in ['WORKER', 'SERVICE']
        data.canBeRunNow = data.state is 'ACTIVE' and data.type in ['SCHEDULED', 'ON_DEMAND'] and data.hasActiveDeploy
        data.canBeBounced = data.state in ['ACTIVE', 'SYSTEM_COOLDOWN'] and data.type in ['WORKER', 'SERVICE']
        data.canBeScaled = data.state in ['ACTIVE', 'SYSTEM_COOLDOWN'] and data.hasActiveDeploy and data.type in ['WORKER', 'SERVICE']

        data

    deletePaused: =>
        $.ajax
            url:  "#{ @url() }/paused"
            type: 'DELETE'

    unpause: (data) =>
        $.ajax
            url:  "#{ @url() }/unpause"
            contentType: 'application/json'
            type: 'POST'
            data: JSON.stringify(
                message: data.message
            )

    pause: (killTasks, duration, message) =>
        data =
            user:      app.getUsername()
            killTasks: killTasks
        if message
            data.message = message
        duration = @_parseDuration(duration)
        if duration
            data.durationMillis = duration
        $.ajax
            url:         "#{ @url() }/pause"
            type:        'POST'
            contentType: 'application/json'
            data: JSON.stringify data

    run: (confirmedOrPromptData, message) ->
        options =
            url: "#{ @url() }/run"
            type: 'POST'
            contentType: 'application/json'
            data: {}

        if typeof confirmedOrPromptData is 'string'
          if confirmedOrPromptData != ''
            options.data.commandLineArgs = [confirmedOrPromptData]
          else
            options.data.commandLineArgs = []
          options.processData = false

        if message
            options.data.message = message
        options.data = JSON.stringify(options.data)
        $.ajax options

    scale: (confirmedOrPromptData) =>
        data =
            instances: confirmedOrPromptData.instances

        if confirmedOrPromptData.message
            data.message = confirmedOrPromptData.message
        duration = @_parseDuration(confirmedOrPromptData.duration)
        if duration
            data.durationMillis = duration
        $.ajax
          url: "#{ @url() }/scale"
          type: "PUT"
          contentType: 'application/json'
          data: JSON.stringify data

    makeScalePermanent: (callback) =>
        $.ajax(
          url: "#{ @url() }/scale"
          type: "DELETE"
        ).then () =>
          @unset('expiringScale')
          callback()

    makePausePermanent: (callback) =>
        $.ajax(
          url: "#{ @url() }/pause"
          type: "DELETE"
        ).then () =>
          @unset('expiringPause')
          callback()

    makeSkipHealthchecksPermanent: (callback) =>
        $.ajax(
          url: "#{ @url() }/skipHealthchecks"
          type: "DELETE"
        ).then () =>
          @unset('expiringSkipHealthchecks')
          callback()

    cancelBounce: (callback) =>
        $.ajax(
          url: "#{ @url() }/bounce"
          type: "DELETE"
        ).then () =>
          @unset('expiringBounce')
          callback()

    bounce: ({incremental, duration, skipHealthchecks, message}) =>
        data = {incremental, skipHealthchecks}
        if message
            data.message = message
        duration = @_parseDuration(duration)
        if duration
            data.durationMillis = duration
        $.ajax
            type: "POST"
            url:  "#{ @url() }/bounce"
            contentType: 'application/json'
            data: JSON.stringify data

    exitCooldown: =>
        $.ajax
            url: "#{ @url() }/exit-cooldown"
            type: "POST"
            contentType: 'application/json'
            data: '{}'

    disableHealthchecks: (message, duration) =>
        data =
            skipHealthchecks: true
        if message
            data.message = message
        duration = @_parseDuration(duration)
        if duration
            data.durationMillis = duration
        $.ajax
            type: "PUT"
            url:  "#{ @url() }/skipHealthchecks"
            contentType: 'application/json'
            data: JSON.stringify data

    enableHealthchecks: (message, duration) =>
        data =
            skipHealthchecks: false
        if message
            data.message = message
        duration = @_parseDuration(duration)
        if duration
            data.durationMillis = duration
        $.ajax
            type: "PUT"
            url:  "#{ @url() }/skipHealthchecks"
            contentType: 'application/json'
            data: JSON.stringify data

    destroy: (message) =>
        data = {}
        if message
            data.message = message
        $.ajax
            type: "DELETE"
            url:  @url()
            contentType: 'application/json'
            data: JSON.stringify(data)

    stepDeploy: (deployId, instances) =>
        data =
            requestId: @get "id"
            deployId: deployId
            targetActiveInstances: instances
        $.ajax
            type: "POST"
            url: "#{ config.apiRoot }/deploys/deploy/#{deployId}/request/#{@get('id')}"
            contentType: 'application/json'
            data: JSON.stringify data

    cancelDeploy: (deployId) =>
        $.ajax
            type: "DELETE"
            url: "#{ config.apiRoot }/deploys/deploy/#{deployId}/request/#{@get('id')}"

    _validateDuration: (duration, action) =>
        if @_parseDuration(duration)
            return true
        else
            vex.dialog.open
                message: 'Invalid duration specified, please try again.'
                callback: (data) ->
                  if data
                      action()
            return false

    _parseDuration: (duration) =>
        if !duration
            return duration
        # Convert strings like '1 hr', '2 days', etc. or any combination thereof to millis
        try
            return juration.parse(duration) * 1000
        catch e
            console.error "Error parsing duration input: #{duration}"
            return null

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
                killTasks = not $('.vex #kill-tasks').is ':checked'
                duration = $('.vex #pause-expiration').val()
                message = $('.vex #pause-message').val()

                if !duration or (duration and @_validateDuration(duration, @promptPause))
                    @pause(killTasks, duration, message).done callback

    promptScale: (callback) =>
        vex.dialog.open
            message: "Enter the desired number of instances to run for request:"
            input:
                scaleTemplate
                    id: @get "id"
                    bounceAfterScale: @get "bounceAfterScale"
                    placeholder: @get 'instances'
            buttons: [
                $.extend _.clone(vex.dialog.buttons.YES), text: 'Scale'
                vex.dialog.buttons.NO
            ]
            afterOpen: ($vexContent) ->
                $vexContent.find('#bounce').click =>
                    if $('.vex #bounce').is ':checked'
                        $(".vex #incremental-bounce-options").show()
                    else
                        $(".vex #incremental-bounce-options").hide()

            callback: (data) =>
                return unless data
                bounce = $('.vex #bounce').is ':checked'
                incremental = $('.vex #incremental-bounce').is ':checked'
                message = $('.vex #scale-message').val()
                duration = $('.vex #scale-expiration').val()
                if !duration or (duration and @_validateDuration(duration, @promptScale))
                    @scale(data).done =>
                        if bounce
                            @bounce({incremental}).done callback
                        else
                            callback()

    promptDisableHealthchecks: (callback) =>
        vex.dialog.open
            message: "Turn <strong>off</strong> healthchecks for this request."
            input: """
                <input name="duration" id="disable-healthchecks-expiration" type="text" placeholder="Expiration (optional)" />
                <span class="help">If an expiration duration is specified, this action will be reverted afterwards. Accepts any english time duration. (Days, Hr, Min...)</span>
                <input name="message" id="disable-healthchecks-message" type="text" placeholder="Message (optional)" />
            """
            buttons: [
                $.extend _.clone(vex.dialog.buttons.YES), text: 'Disable Healthchecks'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return unless data
                duration = $('.vex #disable-healthchecks-expiration').val()
                message = $('.vex #disable-healthchecks-message').val()
                if !duration or (duration and @_validateDuration(duration, @promptDisableHealthchecks))
                    @disableHealthchecks(message, duration).done callback

    promptEnableHealthchecks: (callback) =>
        vex.dialog.open
            message: "Turn <strong>on</strong> healthchecks for this request."
            input: """
                <input name="message" id="disable-healthchecks-message" type="text" placeholder="Message (optional)" />
                <input name="duration" id="disable-healthchecks-expiration" type="text" placeholder="Expiration (optional)" />
                <span class="help">If an expiration duration is specified, this action will be reverted afterwards. Accepts any english time duration. (Days, Hr, Min...)</span>
            """
            buttons: [
                $.extend _.clone(vex.dialog.buttons.YES), text: 'Enable Healthchecks'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return unless data
                duration = $('.vex #disable-healthchecks-expiration').val()
                message = $('.vex #disable-healthchecks-message').val()
                if !duration or (duration and @_validateDuration(duration, @promptEnableHealthchecks))
                    @enableHealthchecks(message, duration).done callback

    promptUnpause: (callback) =>
        vex.dialog.confirm
            message: unpauseTemplate id: @get "id"
            input: """
                <input name="message" id="disable-healthchecks-message" type="text" placeholder="Message (optional)" />
            """
            callback: (confirmed) =>
                return unless confirmed
                @unpause(confirmed).done callback

    promptRun: (callback) =>
        vex.dialog.prompt
            message: "<h3>Run Task</h3>"
            input: runTemplate
                id: @get "id"
                prefix: @localStorageCommandLineInputKeyPrefix
                commands: localStorage.getItem(@localStorageCommandLineInputKeyPrefix + @id)

            buttons: [
                $.extend _.clone(vex.dialog.buttons.YES), text: 'Run now'
                vex.dialog.buttons.NO
            ]

            beforeClose: =>
                return if @data is false

                fileName = @data.filename.trim()
                commandLineInput = @data.commandLineInput.trim()
                message = @data.message

                if fileName.length is 0 and @data.autoTail is 'on'
                    $(window.noFilenameError).removeClass('hide')
                    return false

                else
                    history = localStorage.getItem(@localStorageCommandLineInputKeyPrefix + @id)

                    if history?
                        last = history.split(",")[history.split(",").length - 1]
                        history += ","
                    else
                        history = ""

                    if commandLineInput != last
                        localStorage.setItem(@localStorageCommandLineInputKeyPrefix + @id, history + commandLineInput) if commandLineInput?
                    localStorage.setItem('taskRunRedirectFilename', fileName) if filename?
                    localStorage.setItem('taskRunAutoTail', @data.autoTail)
                    @data.id = @get 'id'

                    @run( @data.commandLineInput, message ).done callback( @data )
                    return true

            afterOpen: =>
                $('#filename').val localStorage.getItem('taskRunRedirectFilename')
                $('#autoTail').prop 'checked', (localStorage.getItem('taskRunAutoTail') is 'on')
                cmdString = localStorage.getItem(@localStorageCommandLineInputKeyPrefix + @id)
                commands = if cmdString then cmdString.split(",").reverse() else []
                $('#commandLineInput').val commands[0]
                localStorage.setItem(@localStorageCommandLineInputKeyPrefix + "historyIndex", 0);
                localStorage.setItem(@localStorageCommandLineInputKeyPrefix + "historyLength", commands.length);

            callback: (data) =>
                @data = data


    promptRerun: (taskId, callback) =>
        task = new TaskHistory {taskId}
        task.fetch()
            .done =>
                command = task.attributes.task.taskRequest.pendingTask.cmdLineArgsList
                vex.dialog.prompt
                    message: "<h3>Rerun Task</h3>"
                    input: runTemplate
                        id: @get "id"
                        command: command
                    buttons: [
                        $.extend _.clone(vex.dialog.buttons.YES), text: 'Run now'
                        vex.dialog.buttons.NO
                    ]

                    beforeClose: =>
                        return if @data is false

                        fileName = @data.filename.trim()
                        commandLineInput = @data.commandLineInput.trim()

                        if fileName.length is 0 and @data.autoTail is 'on'
                            $(window.noFilenameError).removeClass('hide')
                            return false

                        else
                            localStorage.setItem('taskRunRedirectFilename', fileName) if filename?
                            localStorage.setItem('taskRunAutoTail', @data.autoTail)
                            @data.id = @get 'id'

                            @run( @data.commandLineInput ).done callback( @data )
                            return true

                    afterOpen: =>
                        $('#filename').val localStorage.getItem('taskRunRedirectFilename')
                        if command is ""
                            history = localStorage.getItem(@localStorageCommandLineInputKeyPrefix + @id)
                            if !!history
                                history = history.split(",")
                                $('#commandLineInput').val history[history.length - 1]
                        $('#autoTail').prop 'checked', (localStorage.getItem('taskRunAutoTail') is 'on')

                    callback: (data) =>
                        @data = data

    promptRemove: (callback) =>
        vex.dialog.confirm
            message: removeTemplate id: @get "id"
            input: """
                <input name="message" id="disable-healthchecks-message" type="text" placeholder="Message (optional)" />
            """
            callback: (confirmed) =>
                return if not confirmed
                @destroy(confirmed.message).done callback

    promptBounce: (callback) =>
        vex.dialog.confirm
            message: bounceTemplate id: @get "id"
            input: """
                <input name="message" id="bounce-message" type="text" placeholder="Message (optional)" />
            """
            callback: (confirmed) =>
                return if not confirmed
                confirmed.incremental = $('.vex #incremental-bounce').is ':checked'
                confirmed.skipHealthchecks = $('.vex #skip-healthchecks').is ':checked'
                confirmed.duration = $('.vex #bounce-expiration').val()

                if !confirmed.duration or (confirmed.duration and @_validateDuration(confirmed.duration, @promptBounce))
                    @bounce(confirmed).done callback

    promptExitCooldown: (callback) =>
        vex.dialog.confirm
            message: exitCooldownTemplate id: @get "id"
            callback: (confirmed) =>
                return if not confirmed
                @exitCooldown().done callback

    promptStepDeploy: (callback) =>
        pendingDeploy = @get "pendingDeployState"
        nextInstances = Math.min(@get "instances", pendingDeploy.deployProgress.targetActiveInstances + pendingDeploy.deployProgress.deployInstanceCountPerStep)
        vex.dialog.confirm
            message: "<h3>Advance Deploy</h3>"
            input: stepDeployTemplate
                id: pendingDeploy.deployMarker.deployId
                placeholder: nextInstances
                maxInstances: @get "instances"
            callback: (data) =>
                return unless data
                @stepDeploy(pendingDeploy.deployMarker.deployId, data.instances).done callback

    promptCancelDeploy: (callback) =>
        pendingDeploy = @get "pendingDeployState"
        vex.dialog.confirm
            message: cancelDeployTemplate
                id: pendingDeploy.deployMarker.deployId
            callback: (confirmed) =>
                return unless confirmed
                @cancelDeploy(pendingDeploy.deployMarker.deployId).done callback


module.exports = Request
