View = require './view'

TaskHistory = require '../models/TaskHistory'

class TaskView extends View

    template: require './templates/task'

    initialize: =>
        @taskHistory = new TaskHistory {}, taskId: @options.taskId
        @taskHistory.fetch().done => @render()

    render: =>
        return unless @taskHistory.toJSON()?.task?.id

        context =
            taskHistory: @taskHistory.toJSON()

        @$el.html @template context

        utils.setupSortableTables()

module.exports = TaskView