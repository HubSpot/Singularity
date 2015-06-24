Model = require './model'

pauseTemplate = require '../templates/vex/requestPause'
scaleTemplate = require '../templates/vex/requestScale'
unpauseTemplate = require '../templates/vex/requestUnpause'
runTemplate = require '../templates/vex/requestRun'
removeTemplate = require '../templates/vex/requestRemove'
bounceTemplate = require '../templates/vex/requestBounce'

class Request extends Model

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

        data.type = data.request.requestType

        data.instances = data.request.instances or 1
        data.hasMoreThanOneInstance = data.instances > 1

        data.paused = data.state is 'PAUSED'
        data.deleted = data.state is 'DELETED'

        data.hasActiveDeploy = data.activeDeploy? or data.requestDeployState?.activeDeploy?
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

    pause: (killTasks) =>
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
            options.data = JSON.stringify([confirmedOrPromptData])
            options.processData = false

        $.ajax options
        
    scale: (confirmedOrPromptData) =>
        $.ajax
          url: "#{ @url() }/instances?user=#{ app.getUsername() }"
          type: "PUT"
          contentType: 'application/json'
          data:         JSON.stringify
              id:      @get "id"
              instances: confirmedOrPromptData
          
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
                killTasks = not $('.vex #kill-tasks').is ':checked'
                @pause(killTasks).done callback

    promptScale: (callback) =>
        vex.dialog.prompt
            message: scaleTemplate 
                id: @get "id"
            buttons: [
                $.extend _.clone(vex.dialog.buttons.YES), text: 'Scale'
                vex.dialog.buttons.NO
            ]
            placeholder: @get 'instances'
            callback: (data) =>
                return if data is false
                @scale(data).done callback

    promptUnpause: (callback) =>
        vex.dialog.confirm
            message: unpauseTemplate id: @get "id"
            callback: (confirmed) =>
                return unless confirmed
                @unpause().done callback

    promptRun: (callback) =>
        vex.dialog.prompt
            message: ""
            input: runTemplate id: @get "id"
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
                    localStorage.setItem(@localStorageCommandLineInputKeyPrefix + @id, commandLineInput) if commandLineInput?
                    localStorage.setItem('taskRunRedirectFilename', fileName) if filename?
                    localStorage.setItem('taskRunAutoTail', @data.autoTail)
                    @data.id = @get 'id'

                    @run( @data.commandLineInput ).done callback( @data )
                    return true

            afterOpen: => 
                $('#filename').val localStorage.getItem('taskRunRedirectFilename')
                $('#commandLineInput').val localStorage.getItem(@localStorageCommandLineInputKeyPrefix + @id)
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
                @bounce().done callback


module.exports = Request
