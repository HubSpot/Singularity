FormBaseView = require './formBaseView'

Deploy = require 'models/Deploy'

class NewDeployView extends FormBaseView

    template: require '../templates/newDeploy'

    events: ->
        _.extend super,
            'click #executor-type button': 'changeExecutor'

    changeExecutor: (event) ->
        event.preventDefault()

        $target = $ event.currentTarget
        @$('.expandable').addClass 'hide'
        $target.parents('.btn-group').find('.active').removeClass 'active'

        executorType = $target.data 'executor'

        @$("\##{ executorType }-expandable").removeClass 'hide'
        $target.addClass 'active'

    submit: ->
        event.preventDefault()
        return if @$('button[type="submit"]').attr 'disabled'
        @$('.alert').remove()

        deployObject = {}

        deployObject.requestId = @model.id
        deployObject.id        = @$('#id').val()

        deployObject.resources =
            cpus:     parseInt(@valOrNothing '#cpus') or undefined
            memoryMb: @valOrNothing '#memory-mb'
            numPorts: @valOrNothing '#num-ports'

        # Remove resources map if it's empty
        resourceValues = _.values deployObject.resources
        deployObject.resources = undefined if _.isEmpty _.without resourceValues, undefined

        deployObject.serviceBasePath = @valOrNothing '#service-base-path'

        deployObject.healthcheckUri                        = @valOrNothing '#healthcheck-uri'
        deployObject.healthcheckIntervalSeconds            = @valOrNothing '#healthcheck-interval'
        deployObject.healthcheckTimeoutSeconds             = @valOrNothing '#healthcheck-timeout'
        deployObject.skipHealthchecksOnDeploy              = @valOrNothing '#skip-healthcheck'
        deployObject.deployHealthTimeoutSeconds            = @valOrNothing '#deploy-healthcheck-timeout'
        deployObject.considerHealthyAfterRunningForSeconds = @valOrNothing '#consider-healthy-after'

        deployObject.loadBalancerGroups  = @multiList '.lb-group'
        deployObject.loadBalancerOptions = @multiMap '.lb-option'

        deployObject.env = @multiMap '.env'

        command = @$('#command').val()
        executor = @$('#executor-type .active').data 'executor'

        if executor is 'default'
            deployObject.uris    = @multiList '.artifact-uri'
            deployObject.command = command
        else
            deployObject.customExecutorCmd = @valOrNothing '#custom-executor-command'
            deployObject.executorData = {}
            deployObject.executorData.cmd = command

            parseIntList = (list) ->
                _.map list, (string) -> parseInt string

            deployObject.executorData.successfulExitCodes = parseIntList @multiList '.successful-exit-code'
            deployObject.executorData.runningSentinel     = @valOrNothing '#running-sentinel'
            deployObject.executorData.user                = @valOrNothing '#user'
            deployObject.executorData.extraCmdLineArgs    = @multiList '.extra-arg'
            deployObject.executorData.loggingTag          = @valOrNothing '#logging-tag'
            deployObject.executorData.loggingExtraFields  = @multiMap '.extra-field'
            deployObject.executorData.sigKillProcessesAfterMillis = parseInt(@valOrNothing '#kill-after-millis') or 0

        deployModel = new Deploy deployObject, requestId: @model.id
        apiRequest = deployModel.save()

        @lockdown = true
        @$('button[type="submit"]').attr 'disabled', 'disabled'

        apiRequest.error (response) =>
            @postSave()

            app.caughtError()
            @alert "There was a problem: #{ response.responseText }", false
        
        apiRequest.done =>
            @postSave()

            @alert "Deploy successful!"

module.exports = NewDeployView
