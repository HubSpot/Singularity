View = require './view'

TaskHistory = require '../models/TaskHistory'
TaskFiles = require '../collections/TaskFiles'

class FilesView extends View

    template: require './templates/files'

    initialize: ({@taskId, @path}) =>
        @taskFiles = {} # Temporary

        @taskFilesFetchDone = false

        @taskHistory = new TaskHistory {}, taskId: @taskId
        @taskHistory.fetch().done =>
            @render()

            @taskFiles = new TaskFiles {},
                taskId: @taskId
                offerHostname: @taskHistory.attributes.task.offer.hostname
                directory: @taskHistory.attributes.directory
                path: @path

            @taskFiles.fetch().done =>
                @taskFilesFetchDone = true
                @render()

    browse: (@path) =>
        @taskFiles.path = @path

        @taskFilesFetchDone = false
        @taskFiles.fetch().done =>
            @taskFilesFetchDone = true
            @render()

    render: =>
        return @ unless @taskHistory.attributes?.task?.id

        breadcrumbs = []

        breadcrumbs.push
            path: 'files'
            pathRoute: "task/#{ @taskHistory.get('task').id }/files"

        pathSoFar = []
        for path in @path.split('/')
            if path isnt ''
                pathSoFar.push path
                breadcrumbs.push
                    path: path
                    pathRoute: "task/#{ @taskHistory.get('task').id }/files/#{ pathSoFar.join('/') }"

        context =
            taskFilesFetchDone: @taskFilesFetchDone
            taskHistory: @taskHistory.attributes
            taskFiles: _.pluck(@taskFiles.models, 'attributes').reverse()
            breadcrumbs: breadcrumbs

        partials =
            partials:
                filesTable: require './templates/filesTable'

        @$el.html @template context, partials

        utils.setupSortableTables()

        @

module.exports = FilesView