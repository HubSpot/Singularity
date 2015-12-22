Model = require './model'

pauseTemplate = require '../templates/vex/requestPause'
scaleTemplate = require '../templates/vex/requestScale'
unpauseTemplate = require '../templates/vex/requestUnpause'
runTemplate = require '../templates/vex/requestRun'
removeTemplate = require '../templates/vex/requestRemove'
bounceTemplate = require '../templates/vex/requestBounce'
exitCooldownTemplate = require '../templates/vex/exitCooldown'
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

    unpause: =>
        $.ajax
            url:  "#{ @url() }/unpause?user=#{ app.getUsername() }"
            type: 'POST'

    pause: (killTasks, duration) =>
        data =
            user:      app.getUsername()
            killTasks: killTasks
        duration = @_parseDuration(duration)
        if duration
            data.durationMillis = duration
        $.ajax
            url:         "#{ @url() }/pause"
            type:        'POST'
            contentType: 'application/json'
            data: JSON.stringify data

    run: (confirmedOrPromptData) ->
        options =
            url: "#{ @url() }/run?user=#{ app.getUsername() }"
            type: 'POST'
            contentType: 'application/json'

        if typeof confirmedOrPromptData is 'string'
          if confirmedOrPromptData != ''
            options.data = JSON.stringify([confirmedOrPromptData])
          else
            options.data = '[]'
          options.processData = false

        $.ajax options

    scale: (confirmedOrPromptData) =>
        data =
            instances: confirmedOrPromptData.instances
        duration = @_parseDuration(confirmedOrPromptData.duration)
        if duration
            data.durationMillis = duration
        $.ajax
          url: "#{ @url() }/scale?user=#{ app.getUsername() }"
          type: "PUT"
          contentType: 'application/json'
          data: JSON.stringify data

    makeScalePermanent: (callback) =>
        $.ajax(
          url: "#{ @url() }/scale?user=#{ app.getUsername() }"
          type: "DELETE"
        ).then callback

    makePausePermanent: (callback) =>
        $.ajax(
          url: "#{ @url() }/pause?user=#{ app.getUsername() }"
          type: "DELETE"
        ).then callback

    cancelBounce: (callback) =>
        $.ajax(
          url: "#{ @url() }/bounce?user=#{ app.getUsername() }"
          type: "DELETE"
        ).then callback

    bounce: (incremental, duration) =>
        data =
            incremental: incremental
        duration = @_parseDuration(duration)
        if duration
            data.durationMillis = duration
        $.ajax
            type: "POST"
            url:  "#{ @url() }/bounce?user=#{ app.getUsername() }"
            contentType: 'application/json'
            data: JSON.stringify data

    exitCooldown: =>
        $.ajax
            url: "#{ @url() }/exit-cooldown?user=#{ app.getUsername() }"
            type: "POST"

    destroy: =>
        $.ajax
            url:  "#{ @url() }?user=#{ app.getUsername() }"
            type: "DELETE"

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

                if !duration or (duration and @_validateDuration(duration, @promptPause))
                    @pause(killTasks, duration).done callback

    promptScale: (callback) =>
        vex.dialog.open
            message: "Enter the desired number of instances to run for request:"
            input:
                scaleTemplate
                    id: @get "id"
                    bounceAfterScale: @get "bounceAfterScale"
                    placeholder: @get 'instances'
            input: """
                <input name="instances" type="number" placeholder="#{@get 'instances'}" min="1" step="1" required />
                <input name="duration" id="scale-expiration" type="text" placeholder="Expiration (optional)" />
                <span class="help">If an expiration duration is specified, this action will be reverted afterwards. Accepts any english time duration. (Days, Hr, Min...)</span>
            """
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
                duration = $('.vex #scale-expiration').val()
                if !duration or (duration and @_validateDuration(duration, @promptScale))
                    @scale(data).done =>
                        if bounce
                            @bounce(incremental).done callback
                        else
                            callback


    promptUnpause: (callback) =>
        vex.dialog.confirm
            message: unpauseTemplate id: @get "id"
            callback: (confirmed) =>
                return unless confirmed
                @unpause().done callback

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

                    @run( @data.commandLineInput ).done callback( @data )
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
            callback: (confirmed) =>
                return if not confirmed
                @destroy().done callback

    promptBounce: (callback) =>
        vex.dialog.confirm
            message: bounceTemplate id: @get "id"
            callback: (confirmed) =>
                return if not confirmed
                incremental = $('.vex #incremental-bounce').is ':checked'
                duration = $('.vex #bounce-expiration').val()

                if !duration or (duration and @_validateDuration(duration, @promptBounce))
                    @bounce(incremental, duration).done callback

    promptExitCooldown: (callback) =>
        vex.dialog.confirm
            message: exitCooldownTemplate id: @get "id"
            callback: (confirmed) =>
                return if not confirmed
                @exitCooldown().done callback


module.exports = Request
