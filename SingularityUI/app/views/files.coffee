View = require './view'

TaskHistory = require '../models/TaskHistory'
TaskLogFiles = require '../collections/TaskLogFiles'

class FilesView extends View

    template: require './templates/files'

    initialize: ({@taskId, @path}) =>
        @taskLogFiles = {} # Temporary

        @taskHistory = new TaskHistory {}, taskId: @taskId
        @taskHistory.fetch().done =>
            @render()

            @taskLogFiles = new TaskLogFiles {},
                taskId: @taskId
                offerHostname: @taskHistory.attributes.task.offer.hostname
                directory: @taskHistory.attributes.directory
                path: @path

            @taskLogFiles.fetch().done =>
                @render()

    browse: (@path) =>
        @taskLogFiles.path = @path

        @taskLogFiles.fetch().done =>
            @render()

    render: =>
        return unless @taskHistory.attributes?.task?.id

        context =
            taskHistory: @taskHistory.attributes
            taskLogFiles: _.pluck(@taskLogFiles.models, 'attributes').reverse()

        @$el.html @template context

        utils.setupSortableTables()

module.exports = FilesView