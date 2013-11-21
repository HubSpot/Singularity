View = require './view'

TaskHistory = require '../models/TaskHistory'

class TaskView extends View

    template: require './templates/task'

    initialize: =>
        @taskHistory = new TaskHistory {}, taskId: @options.taskId
        @taskHistory.fetch().done => @render()

    render: =>
        return unless @taskHistory.attributes?.task?.id

        context =
            taskHistory: @taskHistory.attributes

        @$el.html @template context

        utils.setupSortableTables()

module.exports = TaskView