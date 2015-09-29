View = require './view'
Task = require '../models/Task'

class TaskView extends View

    baseTemplate: require '../templates/taskDetail/taskBase'

    events: ->
        _.extend super,
            'click [data-action="viewObjectJSON"]': 'viewJson'
            'click [data-action="viewJsonProperty"]': 'viewJsonProperty'
            'click [data-action="remove"]': 'killTask'
            'submit form#runShell': 'executeCommand'
            "change #cmd": "cmdSelected"


    initialize: ({@taskId}) ->
        @subviews.healthcheckNotification.on 'toggleHealthchecks', @toggleHealthchecks

    render: ->
        @$el.html @baseTemplate

        # Plop subview contents in there. It'll take care of everything itself
        @$('#overview').html                    @subviews.overview.$el
        @$('#healthcheck-notification').html    @subviews.healthcheckNotification.$el
        @$('#history').html                     @subviews.history.$el
        @$('#file-browser').html                @subviews.fileBrowser.$el
        @$('#s3-logs').html                     @subviews.s3Logs.$el
        @$('#lb-updates').html                  @subviews.lbUpdates.$el
        @$('#health-checks').html               @subviews.healthChecks.$el
        @$('#info').html                        @subviews.info.$el
        @$('#resources').html                   @subviews.resourceUsage.$el
        @$('#environment').html                 @subviews.environment.$el
        @$('#shell-commands').html              @subviews.shellCommands.$el

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

    killTask: (event) ->
        taskModel = new Task id: @taskId
        taskModel.promptKill =>
            setTimeout (=> @trigger 'refreshrequest'), 1000


    executeCommand: (event) ->
        event.preventDefault()
        cmd = $("#cmd option:selected").text()
        options = $('#cmd-option').val()
        return if @$('button[type="submit"]').attr 'disabled'
        return if !cmd
        taskModel = new Task id: @taskId
        taskModel.runShellCommand(cmd, options)
        $('#cmd-confirm').text('Command Sent')


    cmdSelected: (event) ->
        cmd = config.shellCommands.filter((cmd) ->
          return cmd.name == $("#cmd option:selected").text()
        )[0]
        console.log cmd
        $('.cmd-description').text(cmd.description || '')
        $('#btn_exec').prop("disabled", false)

        options = $('#cmd-option')
        options.empty()
        options.append($("<option></option>").attr("value", option.name)
        .text(option.name + (if option.description then (' (' + option.description + ')') else '')
        )) for option in cmd.options
        options.prop("disabled", !cmd.options)

module.exports = TaskView
