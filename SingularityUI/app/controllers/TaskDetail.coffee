Controller = require './Controller'

TaskHistory = require '../models/TaskHistory'
TaskResourceUsage = require '../models/TaskResourceUsage'

TaskS3Logs = require '../collections/TaskS3Logs'
TaskFiles = require '../collections/TaskFiles'

FileBrowserSubview = require '../views/fileBrowserSubview'

ExpandableTableSubview = require '../views/expandableTableSubview'
SimpleSubview = require '../views/simpleSubview'

TaskView = require '../views/task'

class TaskDetailController extends Controller

    templates:
        overview:      require '../templates/taskDetail/taskOverview'
        history:       require '../templates/taskDetail/taskHistory'
        logs:          require '../templates/taskDetail/taskS3Logs'
        info:          require '../templates/taskDetail/taskInfo'
        environment:   require '../templates/taskDetail/taskEnvironment'
        resourceUsage: require '../templates/taskDetail/taskResourceUsage'

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

        #
        # Subviews
        #
        @subviews.overview = new SimpleSubview
            model:    @models.task
            template: @templates.overview

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

        @subviews.info = new SimpleSubview
            model:    @models.task
            template: @templates.info

        @subviews.environment = new SimpleSubview
            model:    @models.task
            template: @templates.environment

        @subviews.resourceUsage = new SimpleSubview
            model:    @models.resourceUsage
            template: @templates.resourceUsage

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
