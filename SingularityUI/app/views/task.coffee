View = require './view'

TaskHistory = require '../models/TaskHistory'
TaskFiles = require '../collections/TaskFiles'

class TaskView extends View

    template: require './templates/task'

    initialize: =>
        @taskFiles = {} # Temporary

        @taskHistory = new TaskHistory {}, taskId: @options.taskId
        @taskHistory.fetch().done =>
            @render()

            @taskFiles = new TaskFiles {}, { taskId: @options.taskId, offerHostname: @taskHistory.attributes.task.offer.hostname, directory: @taskHistory.attributes.directory }
            @taskFiles.fetch().done =>
                @render()

    render: =>
        return unless @taskHistory.attributes?.task?.id

        context =
            taskHistory: @taskHistory.attributes
            taskFiles: _.pluck(@taskFiles.models, 'attributes').reverse()

        partials =
            partials:
                filesTable: require './templates/filesTable'

        @$el.html @template context, partials

        @setupEvents()

        utils.setupSortableTables()

    setupEvents: =>
        @$el.find('.view-object-json').unbind('click').click (event) ->
            utils.viewJSON 'task', $(event.target).data('task-id')

module.exports = TaskView