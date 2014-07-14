View = require './view'

TaskHistory = require '../models/TaskHistory'
TaskResourceUsage = require '../models/TaskResourceUsage'

TaskS3Logs = require '../collections/TaskS3Logs'
TaskFiles = require '../collections/TaskFiles'

FileBrowserSubview = require './fileBrowserSubview'

ExpandableTableSubview = require './ExpandableTableSubview'

class TaskView extends View

    baseTemplate:          require './templates/taskBase'

    overviewTemplate:      require './templates/taskOverview'
    historyTemplate:       require './templates/taskHistory'

    filesTemplate:         require './templates/taskFiles'
    logsTemplate:          require './templates/taskS3Logs'

    infoTemplate:          require './templates/taskInfo'

    environmentTemplate:   require './templates/taskEnvironment'
    resourceUsageTemplate: require './templates/taskResourceUsage'

    events: ->
        _.extend super,
            'click [data-action="viewObjectJSON"]': 'viewJson'
            'click [data-action="remove"]': 'killTask'

    initialize: ({ @id }) ->
        # Use the history API because it might not be an active task
        @taskHistory = new TaskHistory taskId: @id
        @listenTo @taskHistory, 'sync',  =>
            @renderTask()
            @renderEnvironment()

        @listenTo @taskHistory, 'error', @catchAjaxError

        @taskResourceUsage = new TaskResourceUsage taskId: @id
        @listenTo @taskResourceUsage, 'sync',  @renderResourceUsage
        @listenTo @taskResourceUsage, 'error', @ignoreAjaxError

        @taskFiles = new TaskFiles taskId: @id

        @taskS3Logs = new TaskS3Logs taskId: @id
        @listenTo @taskS3Logs, 'error', @catchAjaxError

        @fileBrowserSubview = new FileBrowserSubview
            collection: @taskFiles

        @s3Subview = new ExpandableTableSubview
            collection: @taskS3Logs
            template:   @logsTemplate

        @refresh()

    refresh: ->
        @taskHistory.fetch()
        @taskResourceUsage.fetch()
        @taskFiles.fetch()
        @s3Subview.fetch()

    ignoreAjaxError: -> app.caughtError()

    catchAjaxError: (collection, response) ->
        if response.status is 404
            app.caughtError()
            @$el.html "<h1>Task does not exist</h1>"
        else if response.status is 501
            app.caughtError()
            @$('[data-s3-logs]').html "<h1>S3 logs not configured</h1>"
            
    render: ->
        # Render the base template only. This is only fired at the start.
        # The different bits of the page are rendered via collection/model events
        # by other functions
        @$el.html @baseTemplate

        # Plot subview contents in there. It'll take care of everything itself
        @$('section[data-s3-logs]').html @s3Subview.$el
        @$('section[data-file-browser]').html @fileBrowserSubview.$el

    renderTask: ->
        # Renders everything taht depends on @taskHistory
        context =
            synced: @taskHistory.synced
            taskHistory: @taskHistory.attributes

        @$('.task-overview-container').html @overviewTemplate context
        @$('.task-info-container').html @infoTemplate context
        @$('.task-history-container').html @historyTemplate context

        utils.setupCopyLinks @$el

    renderEnvironment: ->
        @$('.task-environment-container').html @environmentTemplate
            taskHistory: @taskHistory.attributes

        utils.setupCopyLinks @$el

    renderResourceUsage: ->
        $container = @$ '.task-resource-container'
        # If we refresh and we find that the task was stopped, remove the resources
        if @taskHistory.get('task')?.isStopped
            $container.empty()
        else
            $container.html @resourceUsageTemplate
                taskResourceUsage: @taskResourceUsage.attributes

            utils.setupCopyLinks @$el

    viewJson: (event) ->
        utils.viewJSON 'task', $(event.target).data 'task-id'

    killTask: (event) ->
        taskModel = new Task id: $(event.target).data 'task-id'
        taskModel.promptKill =>
            setTimeout (=> @refresh()), 1000

module.exports = TaskView
