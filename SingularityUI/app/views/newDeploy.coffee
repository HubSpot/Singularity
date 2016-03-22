FormBaseView = require './formBaseView'

Deploy = require 'models/Deploy'
moment = require 'moment'

class NewDeployView extends FormBaseView

    template: require '../templates/newDeploy'

    artifactTemplates:
        embedded: require '../templates/artifactForms/embedded'
        external: require '../templates/artifactForms/external'
        s3:       require '../templates/artifactForms/s3'

    portMapTemplate:
        require '../templates/dockerForms/portMap'

    volumeTemplate:
        require '../templates/dockerForms/volume'

    events: ->
        _.extend super,
            'change #executor-type':                  'changeExecutor'
            'change #container-type':                 'changeContainer'
            'click #artifact-button-row button':      'addArtifact'
            'click .remove-button':                   'removeArtifact'
            'click #docker-port-button-row button':   'addPortMap'
            'click .remove-port-button':              'removePortMap'
            'click #docker-volume-button-row button': 'addVolume'
            'click .remove-volume-button':            'removeVolume'

    changeExecutor: (event) ->
        event.preventDefault()
        executorType = event.currentTarget.options[event.currentTarget.selectedIndex].value

        if executorType is 'default'
            $('.custom-executor').addClass('hide')
            $('.default-executor').removeClass('hide')
        else
            $('.custom-executor').removeClass('hide')
            $('.default-executor').addClass('hide')

    changeContainer: (event) ->
        event.preventDefault()
        containerType = event.currentTarget.options[event.currentTarget.selectedIndex].value

        if containerType is 'docker'
            $('.container-info').removeClass('hide')
        else
            $('.container-info').addClass('hide')

    addArtifact: (event) ->
        event.preventDefault()
        type = $(event.currentTarget).data 'artifact-type'

        $container = @$ '#custom-artifacts'

        $container.append @artifactTemplates[type]
            timestamp: +moment()

    removeArtifact: (event) ->
        event.preventDefault()
        $(event.currentTarget).parent().remove()

    addPortMap: (event) ->
        event.preventDefault()
        $container = @$ '#docker-port-mappings'
        $container.append @portMapTemplate
            timestamp: +moment()

    removePortMap: (event) ->
        event.preventDefault()
        $(event.currentTarget).parent().remove()

    addVolume: (event) ->
        event.preventDefault()
        $container = @$ '#docker-volumes'
        $container.append @volumeTemplate
            timestamp: +moment()

    removeVolume: (event) ->
        event.preventDefault()
        $(event.currentTarget).parent().remove()

    parseIntList: (list) ->
        return undefined if not list
        _.map list, (string) -> parseInt string

    submit: (event) ->
        event.preventDefault()
        return if @$('button[type="submit"]').attr 'disabled'
        @$("[data-alert-location='form']").remove()

        deployObject = {}

        deployObject.requestId = @model.id
        deployObject.id        = @$('#id').val()

        executorType = @$('#executor-type').val()
        command = @$('#command').val()

        if executorType is 'default'
            deployObject.uris    = @multiList '.artifact-uri'
            deployObject.arguments = @multiList '.cmd-line-arg'
            deployObject.command = command
        else
            deployObject.customExecutorCmd = @valOrNothing '#custom-executor-command'
            deployObject.executorData = {}
            deployObject.executorData.cmd = command
            deployObject.executorData.extraCmdLineArgs = @multiList '.extra-arg'
            deployObject.executorData.user = @valOrNothing '#user'
            deployObject.executorData.sigKillProcessesAfterMillis = parseInt(@valOrNothing '#kill-after-millis') or undefined
            deployObject.executorData.successfulExitCodes = @parseIntList @multiList '.successful-exit-code'
            deployObject.executorData.maxTaskThreads = @valOrNothing '#max-task-threads'
            deployObject.executorData.loggingTag = @valOrNothing '#logging-tag'
            deployObject.executorData.loggingExtraFields = @multiMap '.extra-field'
            deployObject.executorData.preserveTaskSandboxAfterFinish = @valOrNothing '#preserve-sandbox'
            deployObject.executorData.skipLogrotateAndCompress = @valOrNothing '#skip-lr-compress'
            deployObject.executorData.loggingS3Bucket = @valOrNothing '#logging-s3-bucket'
            deployObject.executorData.maxOpenFiles = @valOrNothing '#max-open-files'
            deployObject.executorData.runningSentinel = @valOrNothing '#running-sentinel'

            $artifacts = $('.artifact')
            if $artifacts.length
                for $artifact in $artifacts
                    $artifact = $ $artifact

                    type = $artifact.data 'type'
                    if type is 'embedded'
                        deployObject.executorData.embeddedArtifacts = [] unless deployObject.executorData.embeddedArtifacts
                        deployObject.executorData.embeddedArtifacts.push
                            name:     @valOrNothing '.name', $artifact
                            filename: @valOrNothing '.filename', $artifact
                            md5sum:   @valOrNothing '.md5', $artifact
                            content:  @base64Encode @valOrNothing '.content', $artifact
                    else if type is 'external'
                        deployObject.executorData.externalArtifacts = [] unless deployObject.executorData.externalArtifacts
                        deployObject.executorData.externalArtifacts.push
                            name:     @valOrNothing '.name', $artifact
                            filename: @valOrNothing '.filename', $artifact
                            md5sum:   @valOrNothing '.md5', $artifact
                            url:      @valOrNothing '.url', $artifact
                            filesize: parseInt(@valOrNothing '.file-size', $artifact) or undefined
                    else if type is 's3'
                        deployObject.executorData.s3Artifacts = [] unless deployObject.executorData.s3Artifacts
                        deployObject.executorData.s3Artifacts.push
                            name:        @valOrNothing '.name', $artifact
                            filename:    @valOrNothing '.filename', $artifact
                            md5sum:      @valOrNothing '.md5', $artifact
                            s3Bucket:    @valOrNothing '.bucket', $artifact
                            s3ObjectKey: @valOrNothing '.object-key', $artifact
                            filesize:    parseInt(@valOrNothing '.file-size', $artifact) or undefined

        containerType = @$('#container-type').val()
        if containerType is 'docker'
            deployObject.containerInfo = {}
            deployObject.containerInfo.type = 'DOCKER'
            deployObject.containerInfo.docker = {}
            deployObject.containerInfo.docker.image = @$('#docker').val()
            deployObject.containerInfo.docker.network = @$('#dockernetwork').val()
            deployObject.containerInfo.docker.parameters = @multiMap '.docker-paramter'
            deployObject.containerInfo.docker.privileged = @valOrNothing '#privileged'
            deployObject.containerInfo.docker.forcePullImage = @valOrNothing '#force-pull'
            $dockerPorts = $('.docker-port')
            if $dockerPorts.length
                for $dockerPort in $dockerPorts
                    $dockerPort = $ $dockerPort
                    deployObject.containerInfo.docker.portMappings = [] unless deployObject.containerInfo.docker.portMappings
                    deployObject.containerInfo.docker.portMappings.push
                        containerPortType: @valOrNothing '.cont-port-type', $dockerPort
                        containerPort:     @valOrNothing '.cont-port', $dockerPort
                        hostPortType:      @valOrNothing '.host-port-type', $dockerPort
                        hostPort:          @valOrNothing '.host-port', $dockerPort
                        protocol:          @valOrNothing '.protocol', $dockerPort
            $dockerVolumes = $('.docker-volume')
            if $dockerVolumes.length
                for $dockerVolume in $dockerVolumes
                    $dockerVolume = $ $dockerVolume
                    deployObject.containerInfo.volumes = [] unless deployObject.containerInfo.volumes
                    deployObject.containerInfo.volumes.push
                        containerPath: @valOrNothing '.cont-path', $dockerVolume
                        hostPath: @valOrNothing '.host-path', $dockerVolume
                        mode: @valOrNothing '.volume-mode', $dockerVolume

        deployObject.resources =
            cpus:     parseFloat(@valOrNothing '#cpus') or config.defaultCpus
            memoryMb: parseInt(@valOrNothing '#memory-mb') or config.defaultMemory
            numPorts: parseInt(@valOrNothing '#num-ports') or 0

        deployObject.env = @multiMap '.env'

        deployObject.healthcheckUri                        = @valOrNothing '#healthcheck-uri'
        deployObject.healthcheckIntervalSeconds            = @valOrNothing '#healthcheck-interval'
        deployObject.healthcheckTimeoutSeconds             = @valOrNothing '#healthcheck-timeout'
        deployObject.healthcheckPortIndex                  = @valOrNothing '#healthcheck-port-index'
        deployObject.healthcheckMaxTotalTimeoutSeconds     = @valOrNothing '#total-healthcheck-timeout'
        deployObject.deployHealthTimeoutSeconds            = @valOrNothing '#deploy-healthcheck-timeout'
        deployObject.healthCheckProtocol                   = @valOrNothing '#hc-protocol'
        deployObject.skipHealthchecksOnDeploy              = @valOrNothing '#skip-healthcheck'

        deployObject.considerHealthyAfterRunningForSeconds = @valOrNothing '#consider-healthy-after'

        deployObject.serviceBasePath       = @valOrNothing '#service-base-path'
        deployObject.loadBalancerGroups    = @multiList '.lb-group'
        deployObject.loadBalancerOptions   = @multiMap '.lb-option'
        deployObject.loadBalancerPortIndex = @valOrNothing '#lb-port-index'

        deployWrapper = {}
        deployWrapper.deploy = deployObject
        unpauseOnSuccessfulDeploy = @valOrNothing '#deploy-to-unpause'
        if unpauseOnSuccessfulDeploy
            deployWrapper.unpauseOnSuccessfulDeploy = unpauseOnSuccessfulDeploy

        deployModel = new Deploy deployWrapper, requestId: @model.id
        apiRequest = deployModel.save()

        @lockdown = true
        @$('button[type="submit"]').attr 'disabled', 'disabled'

        apiRequest.error (response) =>
            @postSave()

            app.caughtError()
            @alert "There was a problem: #{ response.responseText }", false

        apiRequest.done =>
            @lockdown = false
            @postSave()

            @alert "Deploy successful!"

module.exports = NewDeployView
