View = require './view'

TaskUpdates = require '../collections/TaskUpdates'

class TaskView extends View

    template: require './templates/task'

    initialize: =>
        @task = utils.getAcrossCollections ['tasksActive', 'tasksScheduled'], @options.taskId
        @taskUpdates = new TaskUpdates [], taskId: @options.taskId
        @taskUpdates.fetch().done => @render()

    render: =>
        if not @task
            vex.dialog.alert("<p>Could not open a task by that ID.</p><pre>#{ @options.taskId }</pre>")
            return

        context =
            task: @task.toJSON()
            taskUpdates: @taskUpdates.toJSON()

        @$el.html @template context

module.exports = TaskView