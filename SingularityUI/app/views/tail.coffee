View = require './view'

TaskHistory = require '../models/TaskHistory'
TailerView = require './tailer'

class TailView extends View
    template: require './templates/tail'

    initialize: =>
        @render()

        @taskHistory = new TaskHistory {}, taskId: @options.taskId

        @taskHistory.fetch().done =>
            @tailer = new TailerView
                el: @$el.find 'pre.tailer'
                taskId: @options.taskId
                offerHostname: @taskHistory.attributes.task.offer.hostname
                directory: @taskHistory.attributes.directory
                path: @options.path

    render: =>
        context =
            taskHistory: @taskHistory
            path: @options.path
        @$el.html @template context

module.exports = TailView