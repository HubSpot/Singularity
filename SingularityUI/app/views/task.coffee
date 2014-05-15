View = require './view'

Task = require '../models/Task'
TaskHistory = require '../models/TaskHistory'

TaskS3Logs = require '../collections/TaskS3Logs'
TaskFiles = require '../collections/TaskFiles'

TaskS3LogsTableView = require '../views/taskS3LogsTable'

class TaskView extends View

    template: require './templates/task'

    killTaskTemplate: require './templates/vex/killTask'

    initialize: ->
        @sandboxTries = 0
        @taskFiles = {}
        @taskHistory = new TaskHistory {}, taskId: @options.taskId

        @taskS3Logs = new TaskS3Logs [], taskId: @options.taskId

        $.extend @taskS3Logs,
            totalPages: 100
            totalRecords: 10000
            currentPage: 1
            firstPage: 1
            perPage: 10

    fetch: ->
        deferred = $.Deferred()

        @taskFilesFetchDone = false
        @taskFilesSandboxUnavailable = true

        neverSynced = not @taskHistory.synced

        @taskHistory.fetch().done =>
            @render() if neverSynced

            @taskFiles = new TaskFiles {}, { taskId: @options.taskId, offerHostname: @taskHistory.attributes.task.offer.hostname, directory: @taskHistory.attributes.directory }
            @taskFiles.testSandbox()
                .done(=>
                    @sandboxTries = 0
                    @taskFiles.fetch().done =>
                        @taskFilesFetchDone = true
                        @taskFilesSandboxUnavailable = false
                        deferred.resolve()
                )
                .error(=>
                    @taskFilesFetchDone = true
                    @taskFilesSandboxUnavailable = true
                    deferred.resolve()
                )
            @sandboxTries += 1

        @taskS3Logs.fetch().fail =>
            console.log 's3 logs failed'
        .done =>
            console.log 's3 logs succeeded'

        deferred

    refresh: ->
        @fetch().done =>
            @render()

        # Refresh the current logs table page
        @taskS3Logs.goTo @taskS3Logs.currentPage

        @

    render: ->
        return @ unless @taskHistory.attributes?.task?.id

        console.log "rendering"
        console.trace()

        if @taskHistory.attributes.taskUpdates?.length is 0
            @taskHistory.attributes.hasNoTaskUpdates = true
            setTimeout (=> @refresh()), (1 + Math.pow(1.5, @sandboxTries)) * 1000

        context =
            request: @taskHistory.attributes.task.taskRequest.request
            taskHistory: @taskHistory.attributes
            taskFiles: _.pluck(@taskFiles.models, 'attributes').reverse()
            taskFilesFetchDone: @taskFilesFetchDone
            taskFilesSandboxUnavailable: @taskFilesSandboxUnavailable
            taskS3LogsCollectionJSON: JSON.stringify(@taskS3Logs.toJSON(), null, 4)

        context.taskIdStringLengthTens = Math.floor(context.taskHistory.task.id.length / 10) * 10

        partials =
            partials:
                filesTable: require './templates/filesTable'

        @$el.html @template context, partials

        @setupEvents()
        @setupSubviews()

        @$el.find('pre').each -> utils.setupCopyPre $ @

        @

    setupEvents: =>
        @$el.find('[data-action="viewObjectJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'task', $(e.target).data('task-id')

        @$el.find('[data-action="remove"]').unbind('click').on 'click', (e) =>
            taskModel = new Task id: $(e.target).data('task-id')

            vex.dialog.confirm
                buttons: [
                    $.extend({}, vex.dialog.buttons.YES, (text: 'Kill task', className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'))
                    vex.dialog.buttons.NO
                ]
                message: @killTaskTemplate(taskId: taskModel.get('id'))
                callback: (confirmed) =>
                    return unless confirmed
                    taskModel.destroy()
                    setTimeout (=> @refresh()), 500

    setupSubviews: ->
        if not @taskS3LogsTableView?
            @taskS3LogsTableView = new TaskS3LogsTableView { collection: @taskS3Logs }

        @$('[data-s3-logs-wrapper]').append @taskS3LogsTableView.render().$el

module.exports = TaskView
