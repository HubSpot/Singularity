View = require './view'
Task = require '../models/Task'
TaskFiles = require '../collections/TaskFiles'
TaskHistory = require '../models/TaskHistory'

commandRedirectTemplate = require '../templates/vex/taskCommandRedirect'

interval = (a, b) -> setInterval(b, a)

class TaskView extends View

    baseTemplate: require '../templates/taskDetail/taskBase'

    events: ->
        _.extend super,
            'click [data-action="viewObjectJSON"]': 'viewJson'
            'click [data-action="viewJsonProperty"]': 'viewJsonProperty'
            'submit [data-action="runShell"]': 'executeCommand'
            'change [data-action="cmd"]': 'cmdSelected'


    initialize: ({@taskId}) ->
        @subviews.healthcheckNotification.on 'toggleHealthchecks', @toggleHealthchecks

    render: ->
        @$el.html @baseTemplate

        # Plop subview contents in there. It'll take care of everything itself
        @$('#overview').html                    @subviews.overview.$el
        @$('#alerts').html                      @subviews.alerts.$el
        @$('#task-failure-metadata').html       @subviews.taskFailureMetadata.$el
        @$('#task-warning-metadata').html       @subviews.taskWarningMetadata.$el
        @$('#healthcheck-notification').html    @subviews.healthcheckNotification.$el
        @$('#history').html                     @subviews.history.$el
        @$('#latest-log').html                  @subviews.latestLog.$el
        @$('#file-browser').html                @subviews.fileBrowser.$el
        @$('#s3-logs').html                     @subviews.s3Logs.$el
        @$('#lb-updates').html                  @subviews.lbUpdates.$el
        @$('#health-checks').html               @subviews.healthChecks.$el
        @$('#info').html                        @subviews.info.$el
        @$('#resources').html                   @subviews.resourceUsage.$el
        @$('#environment').html                 @subviews.environment.$el
        @$('#shell-commands').html              @subviews.shellCommands.$el
        @$('#task-info-metadata').html          @subviews.taskInfoMetadata.$el

        super.afterRender()

    toggleHealthchecks: =>
        @subviews.healthChecks.expandToggleIfClosed()

    viewJson: (event) ->
        utils.viewJSON @model

    viewJsonProperty: (event) =>
        index = $(event.target).data('index')
        objKey = $(event.target).data('key')

        # Clone the model so we can use the viewJSON
        # method which requires a model
        modelClone = $.extend true, {}, @model
        modelClone.synced = true

        # remove unwanted attributes and
        # only keep the chosen attribute
        for own key, value of modelClone.attributes
            if key isnt objKey
                modelClone.unset key, {silent:true}
            else
                modelClone.attributes[key].splice 0, modelClone.attributes[key].length, value[index]

        utils.viewJSON modelClone

    executeCommand: (event) ->
        event.preventDefault()
        cmd = $("#cmd option:selected").text()
        options = $('#cmd-option').val()
        return if @$('#btn_exec').attr 'disabled'
        return if !cmd
        taskModel = new Task id: @taskId
        shellRequest = taskModel.runShellCommand(cmd, options)
        shellRequest.success =>
            timestamp = shellRequest.responseJSON.timestamp
            $('#cmd-confirm').text('Command Sent')
            if $("#open-log").is(':checked')
                @executeCommandRedirect()
                @pollForCommandStarted(timestamp)

    cmdSelected: (event) ->
        cmd = config.shellCommands.filter((cmd) ->
          return cmd.name is $("#cmd option:selected").text()
        )[0]

        @subviews.shellCommands.selectedCommandIndex = $("#cmd").prop('selectedIndex')
        @subviews.shellCommands.selectedCommandDescription = cmd.description or ''

        $('.cmd-description').text(cmd.description or '')
        $('#btn_exec').prop("disabled", false)

        options = $('#cmd-option')
        options.empty()
        if cmd.options
            $('#options').removeClass('hidden')
            for option in cmd.options
                options.append($("<option></option>").attr("value", option.name).text(option.name + (if option.description then (' (' + option.description + ')') else '')))
        else
            $('#options').addClass('hidden')
        options.prop("disabled", !cmd.options)

    executeCommandRedirect: ->
        vex.open
            message: "<h3>Waiting for command to run</h3>"
            content: commandRedirectTemplate
            buttons: [
                vex.dialog.buttons.NO
            ]
            beforeClose: =>
                return true

    pollForCommandStarted: (timestamp) =>
        $('#statusText').html('Waiting for command to start...')
        task = new TaskHistory {@taskId}
        @pollInterval = interval 1000, =>
            task.fetch().done =>
                history = task.get('shellCommandHistory');
                if (history and @cmdRequestIsFailed(history, timestamp))
                    clearInterval @pollInterval
                    vex.close()
                    message = @getCmdFailedMessage(history, timestamp)
                    vex.dialog.alert "<h3>Command Failed</h3><p><code>#{message}</code></p>"
                else if (history and @cmdRequestIsStarted(history, timestamp))
                    clearInterval @pollInterval
                    filename = @getCmdRequestOutputFilename(history, timestamp)
                    if filename
                        @pollForCmdFile(filename)
                    else
                        vex.close()  # TODO: show some sort of error

    getCmdRequestOutputFilename: (history, timestamp) ->
        for h in history
            if h.shellRequest.timestamp == timestamp
                for u in h.shellUpdates
                    if u.updateType == 'ACKED'
                        return u.outputFilename

    cmdRequestIsStarted: (history, timestamp) ->
        for h in history
            if h.shellRequest.timestamp == timestamp
                for u in h.shellUpdates
                    if u.updateType == "STARTED"
                        return true
        return false

    cmdRequestIsFailed: (history, timestamp) ->
        for h in history
            if h.shellRequest.timestamp == timestamp
                for u in h.shellUpdates
                    if u.updateType == "FAILED" or u.updateType == "INVALID"
                        return true
        return false

    getCmdFailedMessage: (history, timestamp) ->
        for h in history
            if h.shellRequest.timestamp == timestamp
                for u in h.shellUpdates
                    if u.updateType == "FAILED" or u.updateType == "INVALID"
                        return u.message
        return ''

    pollForCmdFile: (filename) =>
        $('#statusText').html("Waiting for <code>#{ filename }</code> to exist...")
        files = new TaskFiles [], {@taskId}
        @pollInterval = interval 1000, =>
            files.fetch().done =>
                if @containsFile files.models, filename
                    clearInterval @pollInterval
                    vex.close()
                    app.router.navigate "task/#{@taskId}/tail/#{@taskId}/#{ filename }", trigger: true

    containsFile: (files, name) ->
        for file in files
            if file.id is name
                return true
        return false

module.exports = TaskView
