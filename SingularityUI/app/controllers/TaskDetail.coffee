Controller = require './Controller'

TaskHistory = require '../models/TaskHistory'
TaskResourceUsage = require '../models/TaskResourceUsage'

TaskS3Logs = require '../collections/TaskS3Logs'
TaskFiles = require '../collections/TaskFiles'
TaskCleanups = require '../collections/TaskCleanups'
Deploys = require '../collections/Deploys'

FileBrowserSubview = require '../views/fileBrowserSubview'

PaginatedTableClientsideView = require '../views/paginatedTableClientsideView'

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
        shellCommands:              require '../templates/taskDetail/taskShellCommands'
        latestLog:                  require '../templates/taskDetail/taskLatestLog'

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

        @collections.s3Logs = new TaskS3Logs [], {@taskId}

        @collections.taskCleanups = new TaskCleanups

        @collections.pendingDeploys = new Deploys state: 'pending'

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

        @subviews.latestLog = new SimpleSubview
            model:    @models.task
            template: @templates.latestLog

        @subviews.fileBrowser = new FileBrowserSubview
            collection:      @collections.files
            model:           @models.task
            # If we've been given a path we want the files, so scroll directly to it
            scrollWhenReady: @filePath isnt null

        @subviews.s3Logs = new PaginatedTableClientsideView
            collection:     @collections.s3Logs
            template:       @templates.logs

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

        @subviews.shellCommands = new SimpleSubview
            model: @models.task
            template: @templates.shellCommands

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

    refresh: ->
        @resourcesFetched = false

        @collections.taskCleanups.fetch()

        @collections.pendingDeploys.fetch()

        @models.task.fetch()
            .done =>
                @fetchResourceUsage() if @models.task.get('isStillRunning')
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
