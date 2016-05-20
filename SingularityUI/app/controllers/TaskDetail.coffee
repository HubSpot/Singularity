Controller = require './Controller'

TaskHistory = require '../models/TaskHistory'
TaskResourceUsage = require '../models/TaskResourceUsage'

TaskS3Logs = require '../collections/TaskS3Logs'
TaskFiles = require '../collections/TaskFiles'
TaskCleanups = require '../collections/TaskCleanups'
Deploys = require '../collections/Deploys'
DeployDetails = require '../models/DeployDetails'
Alerts = require '../collections/Alerts'

FileBrowserSubview = require '../views/fileBrowserSubview'
ExpandableTableSubview = require '../views/expandableTableSubview'
OverviewSubview = require '../views/taskOverviewSubview'
HealthcheckNotification = require '../views/taskHealthcheckNotificationSubview'
SimpleSubview = require '../views/simpleSubview'
TaskMetadataAlertSubview = require '../views/taskMetadataAlertSubview'
ShellCommands = require '../views/taskShellCommandsSubview'
LatestLog = require '../views/taskLatestLogSubview'

TaskView = require '../views/task'

class TaskDetailController extends Controller

    templates:
        overview:                     require '../templates/taskDetail/taskOverview'
        deployFailureNotification:    require '../templates/taskDetail/taskDeployFailureNotification'
        healthcheckNotification:      require '../templates/taskDetail/taskHealthcheckNotification'
        taskMetadataAlert:            require '../templates/taskDetail/taskMetadataAlert'
        history:                      require '../templates/taskDetail/taskHistory'
        logs:                         require '../templates/taskDetail/taskS3Logs'
        lbUpdates:                    require '../templates/taskDetail/taskLbUpdates'
        healthChecks:                 require '../templates/taskDetail/taskHealthChecks'
        info:                         require '../templates/taskDetail/taskInfo'
        environment:                  require '../templates/taskDetail/taskEnvironment'
        resourceUsage:                require '../templates/taskDetail/taskResourceUsage'
        alerts:                       require '../templates/alerts'
        latestLog:                    require '../templates/taskDetail/taskLatestLog'
        shellCommands:                require '../templates/taskDetail/taskShellCommands'
        taskMetadataTable:            require '../templates/taskDetail/taskMetadataTable'

    initialize: ({@taskId, @filePath}) ->
        @title @taskId

        #
        # Models / collections
        #
        # Use the history API because it might not be an active task
        @models.task          = new TaskHistory {@taskId}
        @models.resourceUsage = new TaskResourceUsage {@taskId}

        # Files for the files browser
        @collections.files = new TaskFiles [],
            taskId: @taskId
            path:   @filePath

        # Files where we expect the log to be
        @collections.logDirectory = new TaskFiles [],
            taskId: @taskId
            path:   undefined

        @collections.s3Logs = new TaskS3Logs [], {@taskId}

        @collections.taskCleanups = new TaskCleanups

        @collections.pendingDeploys = new Deploys state: 'pending'

        @collections.alerts = new Alerts

        #
        # Subviews
        #
        @subviews.overview = new OverviewSubview
            collection: @collections.taskCleanups
            model:      @models.task
            template:   @templates.overview

        @subviews.deployFailureNotification = new SimpleSubview
            model: @models.task
            template: @templates.deployFailureNotification
            extraRenderData: (subView) =>
                { deploy: if @deploy then @deploy.toJSON() else '' }

        @subviews.healthcheckNotification = new HealthcheckNotification
            model:          @models.task
            template:       @templates.healthcheckNotification
            pendingDeploys: @collections.pendingDeploys

        @subviews.taskErrorMetadata = new TaskMetadataAlertSubview
            model: @models.task
            template: @templates.taskMetadataAlert
            level: 'ERROR'
            alertClass: 'danger'
            numberPerPage: 5

        @subviews.taskWarnMetadata = new TaskMetadataAlertSubview
            model: @models.task
            template: @templates.taskMetadataAlert
            level: 'WARN'
            alertClass: 'warning'
            numberPerPage: 1

        @subviews.history = new SimpleSubview
            model:    @models.task
            template: @templates.history

        @subviews.latestLog = new LatestLog
            task:      @models.task
            logDir:    @collections.logDirectory
            template:  @templates.latestLog

        @subviews.fileBrowser = new FileBrowserSubview
            collection:      @collections.files
            model:           @models.task
            # If we've been given a path we want the files, so scroll directly to it
            scrollWhenReady: @filePath isnt null
            slaveOffline: false

        @subviews.s3Logs = new ExpandableTableSubview
            collection: @collections.s3Logs
            template:   @templates.logs

        @subviews.lbUpdates = new SimpleSubview
            model:    @models.task
            template:   @templates.lbUpdates

        @subviews.healthChecks = new SimpleSubview
            model:    @models.task
            template:   @templates.healthChecks

        @subviews.info = new SimpleSubview
            model:    @models.task
            template: @templates.info

        @subviews.environment = new SimpleSubview
            model:    @models.task
            template: @templates.environment

        @subviews.resourceUsage = new SimpleSubview
            model:    @models.resourceUsage
            template: @templates.resourceUsage

        @subviews.alerts = new SimpleSubview
            collection:    @collections.alerts
            template:      @templates.alerts

        @subviews.shellCommands = new ShellCommands
            model: @models.task
            template: @templates.shellCommands

        @subviews.taskMetadataTable = new SimpleSubview
            model: @models.task
            template: @templates.taskMetadataTable

        #
        # Getting stuff in gear
        #
        @setView new TaskView _.extend {@subviews, @taskId},
            model: @models.task

        @refresh()

        app.showView @view


    fetchResourceUsage: ->
        @models.resourceUsage?.fetch()
            .done =>
                # Store current resource usage to compare against future resource usage
                @models.resourceUsage.setCpuUsage() if @models.resourceUsage.get('previousUsage')
                @models.resourceUsage.set('previousUsage', @models.resourceUsage.toJSON())

                if not @resourcesFetched
                    setTimeout (=> @fetchResourceUsage() ), 2000
                    @resourcesFetched = true

            .error =>
                # If this 404s there's nothing to get so don't bother
                app.caughtError()
                delete @models.resourceUsage

    getAlerts: =>
        alerts = []
        task = @models.task
        requestId = @models.task.attributes.task.taskRequest.request.id
        deployId = @models.task.attributes.task.taskRequest.deploy.id

        # Is this a scheduled task that has been running much longer than previous ones?
        if task.attributes.task.taskRequest.request.requestType == 'SCHEDULED' and task.get('isStillRunning')
            deployInfo = new DeployDetails
              deployId: deployId
              requestId: requestId
            deployPromise = deployInfo.fetch()
            deployPromise.done =>
                avg = deployInfo.get('deployStatistics')?.averageRuntimeMillis
                current =  new Date().getTime() - task.get('task').taskId.startedAt
                threshold = window.config.warnIfScheduledJobIsRunningPastNextRunPct / 100
                # Alert if current uptime is longer than the average * the configurable percentage
                if current > (avg * threshold)
                    alerts.push
                      title: 'Warning:',
                      message: "This scheduled task has been running longer than <code>#{threshold}</code> times the average for the request and may be stuck.",
                      level: 'warning'
        # Was this task killed by a decommissioning slave?
        if !task.get('isStillRunning')
            updates = task.get('taskUpdates')
            decomMessage = updates.filter (u) =>
                return u.statusMessage?.indexOf('DECOMISSIONING') != -1 and u.taskState == 'TASK_CLEANING'
            killedMessage = updates.filter (u) =>
                return u.taskState == 'TASK_KILLED'
            if decomMessage.length > 0 and killedMessage.length > 0
                alerts.push
                  message: 'This task was replaced then killed by Singularity due to a slave decommissioning.',
                  level: 'warning'

        if deployPromise
            deployPromise.done =>
                @collections.alerts.reset(alerts)
        else
            @collections.alerts.reset(alerts)

    fetchDeployDetails: ->
        @deploy = new DeployDetails
            deployId: @models.task.attributes.task.taskId.deployId
            requestId: @models.task.attributes.task.taskId.requestId
        @deploy.fetch()
            .success =>
                @subviews.deployFailureNotification.render()
                @subviews.healthcheckNotification.deploy = @deploy
                @subviews.healthcheckNotification.render()
            .error =>
                app.caughtError()

    refresh: ->
        @resourcesFetched = false

        @collections.taskCleanups.fetch()

        @collections.pendingDeploys.fetch()

        @models.task.fetch()
            .done =>
                @collections.files.fetch().error @ignore404
                @fetchResourceUsage() if @models.task.get('isStillRunning')
                logPath = if @models.task.get('isStillRunning') then config.runningTaskLogPath else config.finishedTaskLogPath
                logPath = logPath.replace('$TASK_ID', @taskId)
                logPath = _.initial(logPath.split('/')).join('/')
                @collections.logDirectory.path = logPath
                @collections.logDirectory.fetch().error (response) =>
                    @ignore400 response
                    @ignore404 response
                    @subviews.fileBrowser.slaveOffline = true and @subviews.fileBrowser.render() if response.status is 404
            .success =>
                @getAlerts()
                @fetchDeployDetails()
            .error =>
                # If this 404s the task doesn't exist
                app.caughtError()
                app.router.notFound()

        if @collections.s3Logs?.currentPage is 1
            @collections.s3Logs.fetch().error =>
                # It probably means S3 logs haven't been configured
                app.caughtError()
                delete @collections.s3Logs

module.exports = TaskDetailController
