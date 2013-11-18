View = require './view'

class TaskView extends View

    template: require './templates/task'

    initialize: =>
        @task = utils.getAcrossCollections ['tasksActive', 'tasksScheduled'], @options.taskId

    render: =>
        if not @task
            vex.dialog.alert('Could not open a task by that ID. Ask <b>@wsorenson</b>...')
            return

        context =
            task: @task.toJSON()

        @$el.html @template context

module.exports = TaskView