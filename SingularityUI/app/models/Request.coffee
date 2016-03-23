Model = require './model'

Racks = require '../collections/Racks'

pauseTemplate = require '../templates/vex/requestPause'
scaleTemplate = require '../templates/vex/requestScale'
scaleEvenNumbersTemplate = require '../templates/vex/requestScaleConfirmRacks'
unpauseTemplate = require '../templates/vex/requestUnpause'
runTemplate = require '../templates/vex/requestRun'
removeTemplate = require '../templates/vex/requestRemove'
bounceTemplate = require '../templates/vex/requestBounce'
exitCooldownTemplate = require '../templates/vex/exitCooldown'
stepDeployTemplate = require '../templates/vex/stepDeploy'
cancelDeployTemplate = require '../templates/vex/cancelDeploy'
TaskHistory = require '../models/TaskHistory'

vex = require 'vex.dialog'
juration = require 'juration'

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

    unpause: (data) =>
        $.ajax
            url:  "#{ @url() }/unpause"
            contentType: 'application/json'
            type: 'POST'
            data: JSON.stringify(
                message: data.message
            )

    hideEvenNumberAcrossRacksHint: (callback) ->
        @attributes.request.hideEvenNumberAcrossRacksHint = true
        ajaxPromise = $.ajax(
            type: 'POST'
            url: "#{ config.apiRoot }/requests"
            contentType: 'application/json'
            data: JSON.stringify @attributes.request
        )
        ajaxPromise.then callback

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

    run: (cmdLineArgs, message) ->
        options =
            url: "#{ @url() }/run"
            type: 'POST'
            contentType: 'application/json'
            data: {}

        options.data.commandLineArgs = cmdLineArgs

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
            url: "#{ config.apiRoot }/deploys/update"
            contentType: 'application/json'
            data: JSON.stringify data

    cancelDeploy: (deployId) =>
        $.ajax
            type: "DELETE"
            url: "#{ config.apiRoot }/deploys/deploy/#{deployId}/request/#{@get('id')}"

    _validateDuration: (duration, action, callback) =>
        if @_parseDuration(duration)
            return true
        else
            vex.dialog.open
                message: 'Invalid duration specified, please try again.'
                callback: (data) ->
                  if data
                      action(callback)
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

                if !duration or (duration and @_validateDuration(duration, @promptPause, callback))
                    @pause(killTasks, duration, message).done callback

    callScale: (data, bounce, incremental, message, duration, callback, setHideEvenNumberAcrossRacksHintTrue) =>
        @scale(data).done =>
            if setHideEvenNumberAcrossRacksHintTrue
                @attributes.request.instances = data.instances
                @hideEvenNumberAcrossRacksHint () =>
                    if bounce 
                        @bounce({incremental}).done callback
                    else
                        callback()
            else if bounce 
                @bounce({incremental}).done callback
            else
                callback()

    promptScaleEvenNumberRacks: (scaleData) =>
        vex.dialog.open
            message: scaleEvenNumbersTemplate
                instances: parseInt(scaleData.data.instances)
                notOneInstance: parseInt(scaleData.data.instances) != 1
                racks: @racks.length
                notOneRack: @racks.length != 1
                mod: scaleData.mod
                modNotOne: scaleData.mod != 1
                lower: parseInt(scaleData.data.instances) - scaleData.mod
                higher: parseInt(scaleData.data.instances) + @racks.length - scaleData.mod
                config: config
            input: """
                
            """
            buttons: [
                $.extend _.clone(vex.dialog.buttons.YES), text: "Scale"
                vex.dialog.buttons.NO
            ]
            scaleData: scaleData # Not sure why this is necessary, callback for whatever reason doesn't have access to the function's variables
            callback: (data) =>
                return unless data
                scaleData.data.instances = data.instances
                @callScale scaleData.data, scaleData.bounce, scaleData.incremental, scaleData.message, scaleData.duration, scaleData.callback, data.optOut
                

    checkScaleEvenNumberRacks: (data, bounce, incremental, message, duration, callback) =>
        mod = data.instances %% @racks.length
        if mod
            @promptScaleEvenNumberRacks 
                callback: callback
                data: data
                mod: mod 
                bounce: bounce
                incremental: incremental
                message: message
                duration: duration
        else
            @callScale data, bounce, incremental, message, duration, callback, false

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
                if !duration or (duration and @_validateDuration(duration, @promptScale, callback))
                    if @attributes.request.rackSensitive and not @attributes.request.hideEvenNumberAcrossRacksHint
                        if @racks
                            @checkScaleEvenNumberRacks data, bounce, incremental, message, duration, callback
                        else
                            @racks = new Racks []
                            @racks.fetch
                                success: () => @checkScaleEvenNumberRacks data, bounce, incremental, message, duration, callback
                                error: () => 
                                    app.caughtError() # Since we scale anyway, don't show the error
                                    @callScale data, bounce, incremental, message, duration, callback, false
                    else
                        @callScale data, bounce, incremental, message, duration, callback, false
                    

    promptDisableHealthchecksDuration: (message, duration, callback) =>
        durationMillis = @_parseDuration(duration)
        if durationMillis < 3600000
            vex.dialog.confirm
                message: '
                    <strong>Are you sure you want to disable healthchecks for less than an hour?</strong>
                    This may not be enough time for your service to get into a stable state.
                '
                buttons: [
                    $.extend _.clone(vex.dialog.buttons.YES), text: 'Disable Healthchecks'
                    vex.dialog.buttons.NO
                ]
                callback: (data) =>
                    if data
                        @disableHealthchecks(message, duration).done callback
        else
            @disableHealthchecks(message, duration).done callback

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
                if !duration
                    @disableHealthchecks(message, duration).done callback
                else if @_validateDuration(duration, @promptDisableHealthchecks, callback)
                    @promptDisableHealthchecksDuration(message, duration, callback)


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
                if !duration or (duration and @_validateDuration(duration, @promptEnableHealthchecks, callback))
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
        try
            lastCommands = JSON.parse(localStorage.getItem(@localStorageCommandLineInputKeyPrefix + @id))
        catch e
            console.error('Could not parse previous commands JSON')
            lastCommands = []
        vex.dialog.prompt
            message: "<h3>Run Task</h3>"
            input: runTemplate
                id: @get "id"
                prefix: @localStorageCommandLineInputKeyPrefix
                commands: if lastCommands? then lastCommands[lastCommands.length - 1] else []

            buttons: [
                $.extend _.clone(vex.dialog.buttons.YES), text: 'Run now'
                vex.dialog.buttons.NO
            ]

            beforeClose: =>
                return if @data is false

                fileName = @data.filename.trim()
                message = @data.message

                if fileName and fileName.length is 0 and @data.autoTail is 'on'
                    $(window.noFilenameError).removeClass('hide')
                    return false

                else
                    if @data.commandLineInput?
                        try
                            history = JSON.parse(localStorage.getItem(@localStorageCommandLineInputKeyPrefix + @id))
                        catch e
                            console.error('Could not parse previous command history')
                            history = []
                        if history and @data.commandLineInput != history[-1]
                            history.push(@data.commandLineInput)
                            localStorage.setItem(@localStorageCommandLineInputKeyPrefix + @id, JSON.stringify(history))
                    localStorage.setItem('taskRunRedirectFilename', fileName) if filename?
                    localStorage.setItem('taskRunAutoTail', @data.autoTail)
                    @data.id = @get 'id'

                    @run( @data.commandLineInput, message ).done callback( @data )
                    return true

            afterOpen: =>
                $('#filename').val localStorage.getItem('taskRunRedirectFilename')
                $('#autoTail').prop 'checked', (localStorage.getItem('taskRunAutoTail') is 'on')
                $('#add-cmd-line-arg').on('click', { removeCmdLineArg: @removeCmdLineArg }, @addCmdLineArg)
                $('.remove-button').click @removeCmdLineArg

            callback: (data) =>
                if data.commandLineInput
                    if typeof data.commandLineInput is 'string'
                        if data.commandLineInput != ''
                            data.commandLineInput = [data.commandLineInput.trim()]
                        else
                            data.commandLineInput = []
                    if data.commandLineInput.length == 1 and data.commandLineInput[0] == ''
                        data.commandLineInput = []
                else
                    data.commandLineInput = []
                @data = data

    addCmdLineArg: (event) ->
        event.preventDefault()
        $container = $('#cmd-line-inputs')
        $container.append """
        <div class="cmd-line-arg">
            <div class="remove-button"></div>
            <input id="commandLineInput" name="commandLineInput" type="text" class="vex-dialog-prompt-input" placeholder=""/>
        </div>
        """
        $('.remove-button').click event.data.removeCmdLineArg

    removeCmdLineArg: (event) ->
        event.preventDefault()
        $(event.currentTarget).parent().remove()

    promptRerun: (taskId, callback) =>
        task = new TaskHistory {taskId}
        task.fetch()
            .done =>
                commands = task.attributes.task.taskRequest.pendingTask.cmdLineArgsList
                vex.dialog.prompt
                    message: "<h3>Rerun Task</h3>"
                    input: runTemplate
                        id: @get "id"
                        commands: commands
                    buttons: [
                        $.extend _.clone(vex.dialog.buttons.YES), text: 'Run now'
                        vex.dialog.buttons.NO
                    ]

                    beforeClose: =>
                        return if @data is false

                        fileName = @data.filename.trim()
                        message = @data.message

                        if fileName and fileName.length is 0 and @data.autoTail is 'on'
                            $(window.noFilenameError).removeClass('hide')
                            return false

                        else
                            localStorage.setItem('taskRunRedirectFilename', fileName) if filename?
                            localStorage.setItem('taskRunAutoTail', @data.autoTail)
                            @data.id = @get 'id'

                            @run( @data.commandLineInput, message ).done callback( @data )
                            return true

                    afterOpen: =>
                        $('#filename').val localStorage.getItem('taskRunRedirectFilename')
                        $('#autoTail').prop 'checked', (localStorage.getItem('taskRunAutoTail') is 'on')
                        $('#add-cmd-line-arg').on('click', { removeCmdLineArg: @removeCmdLineArg }, @addCmdLineArg)
                        $('.remove-button').click @removeCmdLineArg

                    callback: (data) =>
                        if data.commandLineInput
                            if typeof data.commandLineInput is 'string'
                                if data.commandLineInput != ''
                                    data.commandLineInput = [data.commandLineInput.trim()]
                                else
                                    data.commandLineInput = []
                            if data.commandLineInput.length == 1 and data.commandLineInput[0] == ''
                                data.commandLineInput = []
                        else
                            data.commandLineInput = []
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
            message: bounceTemplate
                id: @get "id"
                config: config
            input: """
                <input name="message" id="bounce-message" type="text" placeholder="Message (optional)" />
            """
            callback: (confirmed) =>
                return if not confirmed
                confirmed.incremental = $('.vex #incremental-bounce').is ':checked'
                confirmed.skipHealthchecks = $('.vex #skip-healthchecks').is ':checked'
                confirmed.duration = $('.vex #bounce-expiration').val()

                if !confirmed.duration or (confirmed.duration and @_validateDuration(confirmed.duration, @promptBounce, callback))
                    @bounce(confirmed).done callback

    promptExitCooldown: (callback) =>
        vex.dialog.confirm
            message: exitCooldownTemplate id: @get "id"
            callback: (confirmed) =>
                return if not confirmed
                @exitCooldown().done callback

    promptStepDeploy: (callback) =>
        pendingDeploy = @get "pendingDeployState"
        nextInstances = pendingDeploy.deployProgress.targetActiveInstances + pendingDeploy.deployProgress.deployInstanceCountPerStep
        maxInstances = @get "instances"
        if maxInstances < nextInstances
            nextInstances = maxInstances
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
