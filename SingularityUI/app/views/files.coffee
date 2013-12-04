View = require './view'

TaskHistory = require '../models/TaskHistory'
TaskFiles = require '../collections/TaskFiles'

class FilesView extends View

    template: require './templates/files'

    initialize: ({@taskId, @path}) =>
        @taskFiles = {} # Temporary

        @taskHistory = new TaskHistory {}, taskId: @taskId
        @taskHistory.fetch().done =>
            @render()

            @taskFiles = new TaskFiles {},
                taskId: @taskId
                offerHostname: @taskHistory.attributes.task.offer.hostname
                directory: @taskHistory.attributes.directory
                path: @path

            @taskFiles.fetch().done =>
                @render()

    browse: (@path) =>
        @taskFiles.path = @path

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

        utils.setupSortableTables()

module.exports = FilesView