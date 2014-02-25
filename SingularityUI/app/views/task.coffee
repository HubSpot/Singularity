View = require './view'

Task = require '../models/Task'
TaskHistory = require '../models/TaskHistory'

TaskFiles = require '../collections/TaskFiles'

class TaskView extends View

    template: require './templates/task'

    killTaskTemplate: require './templates/vex/killTask'

    initialize: =>
        @taskFiles = {}

        @taskFilesFetchDone = false
        @taskFilesSandboxUnavailable = false

        @taskHistory = new TaskHistory {}, taskId: @options.taskId
        @taskHistory.fetch().done =>
            @render()

            @taskFiles = new TaskFiles {}, { taskId: @options.taskId, offerHostname: @taskHistory.attributes.task.offer.hostname, directory: @taskHistory.attributes.directory }
            # @taskFiles.fetch().done =>
            #     @taskFilesFetchDone = true
            #     @render()

            log 'asdasdasd'
            #log @taskFiles.fetch()

            @taskFiles.testSandbox()
                .done(=>
                    log 'done'
                    @taskFiles.fetch().done =>
                        @taskFilesFetchDone = true
                        @render()
                )
                .error(=>
                    @taskFilesFetchDone = true
                    @taskFilesSandboxUnavailable = true
                    @render()
                )

    render: =>
        return @ unless @taskHistory.attributes?.task?.id

        context =
            request: @taskHistory.attributes.task.taskRequest.request
            taskHistory: @taskHistory.attributes
            taskFiles: _.pluck(@taskFiles.models, 'attributes').reverse()
            taskFilesFetchDone: @taskFilesFetchDone
            taskFilesSandboxUnavailable: @taskFilesSandboxUnavailable

        partials =
            partials:
                filesTable: require './templates/filesTable'

        @$el.html @template context, partials

        @setupEvents()

        utils.setupSortableTables()

        @

    setupEvents: =>
        @$el.find('[data-action="viewObjectJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'task', $(e.target).data('task-id')

        @$el.find('[data-action="remove"]').unbind('click').on 'click', (e) =>
            taskModel = new Task id: $(e.target).data('task-id')

            vex.dialog.confirm
                message: @killTaskTemplate(taskId: taskModel.get('id'))
                callback: (confirmed) =>
                    return unless confirmed
                    taskModel.destroy()
                    app.router.navigate 'tasks', trigger: true

module.exports = TaskView