Controller = require './Controller'

TaskHistory = require '../models/TaskHistory'
TaskResourceUsage = require '../models/TaskResourceUsage'

TaskS3Logs = require '../collections/TaskS3Logs'
TaskFiles = require '../collections/TaskFiles'
TaskCleanups = require '../collections/TaskCleanups'
Deploys = require '../collections/Deploys'
RequestHistoricalTasks = require '../collections/RequestHistoricalTasks'
Alerts = require '../collections/Alerts'

FileBrowserSubview = require '../views/fileBrowserSubview'
ExpandableTableSubview = require '../views/expandableTableSubview'
OverviewSubview = require '../views/taskOverviewSubview'
HealthcheckNotification = require '../views/taskHealthcheckNotificationSubview'
SimpleSubview = require '../views/simpleSubview'

TaskView = require '../views/task'

class TaskDetailController extends Controller

    templates:
        overview:                   require '../templates/taskDetail/taskOverview'
        healthcheckNotification:    require '../templates/taskDetail/taskHealthcheckNotification'
        history:                    require '../templates/taskDetail/taskHistory'
        logs:                       require '../templates/taskDetail/taskS3Logs'
        lbUpdates:                  require '../templates/taskDetail/taskLbUpdates'
        healthChecks:               require '../templates/taskDetail/taskHealthChecks'
        info:                       require '../templates/taskDetail/taskInfo'
        environment:                require '../templates/taskDetail/taskEnvironment'
        resourceUsage:              require '../templates/taskDetail/taskResourceUsage'
        alerts:                     require '../templates/alerts'

    initialize: ({@taskId, @filePath}) ->
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

        @subviews.healthcheckNotification = new HealthcheckNotification
            model:          @models.task
            template:       @templates.healthcheckNotification
            pendingDeploys: @collections.pendingDeploys

        @subviews.history = new SimpleSubview
            model:    @models.task
            template: @templates.history

        @subviews.fileBrowser = new FileBrowserSubview
            collection:      @collections.files
            model:           @models.task
            # If we've been given a path we want the files, so scroll directly to it
            scrollWhenReady: @filePath isnt null

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

        #
        # Getting stuff in gear
        #
        @setView new TaskView _.extend {@subviews, @taskId},
            model: @models.task

        @refresh()
        @collections.files.fetch().error @ignore404

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

    getAlerts: (taskHistory) =>
        alerts = []
        # Is this a scheduled task that has been running much longer than previous ones?
        if @models.task.attributes.task.taskRequest.request.requestType == 'SCHEDULED' and @models.task.get('isStillRunning')
            times = taskHistory.models.map (t) =>
                return t.get('updatedAt') - t.get('taskId').startedAt
            avg = times.reduce (p, c) ->
                return p + c

            avg = Math.round avg / times.length
            current =  new Date().getTime() - @models.task.get('task').taskId.startedAt
            # Alert if current uptime is longer than twice the average
            if current > (avg * 2)
                alerts.push
                  title: 'Warning:',
                  message: 'This scheduled task has been running longer than twice the average for the request and may be stuck.',
                  level: 'warning'
        return alerts

    refresh: ->
        @resourcesFetched = false

        @collections.taskCleanups.fetch()

        @collections.pendingDeploys.fetch()

        @models.task.fetch()
            .done =>
                @fetchResourceUsage() if @models.task.get('isStillRunning')
            .success =>
                requestId = @models.task.attributes.task.taskRequest.request.id
                taskHistory = new RequestHistoricalTasks [], {requestId}
                taskHistory.fetch().success =>
                    alerts = @getAlerts taskHistory
                    @collections.alerts.reset()
                    for alert in alerts
                        @collections.alerts.add alert
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
