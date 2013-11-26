View = require './view'

TaskHistory = require '../models/TaskHistory'
TaskLogFiles = require '../collections/TaskLogFiles'

class TaskView extends View

    template: require './templates/task'

    initialize: =>
        @taskLogFiles = {} # Temporary

        @taskHistory = new TaskHistory {}, taskId: @options.taskId
        @taskHistory.fetch().done =>
            @render()

            @taskLogFiles = new TaskLogFiles {}, { offerHostname: @taskHistory.attributes.task.offer.hostname, directory: @taskHistory.attributes.directory }
            @taskLogFiles.fetch().done =>
                @render()

    render: =>
        return unless @taskHistory.attributes?.task?.id

        context =
            taskHistory: @taskHistory.attributes
            taskLogFiles: _.pluck(@taskLogFiles.models, 'attributes').reverse()

        @$el.html @template context

        utils.setupSortableTables()

module.exports = TaskView