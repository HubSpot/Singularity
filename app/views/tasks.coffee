View = require './view'

class TasksView extends View

    template: require './templates/tasks'

    render: =>
        context =
            tasksActive: app.collections.tasksActive.toJSON()
            tasksScheduled: app.collections.tasksScheduled.toJSON()

        @$el.html @template context

        @setupEvents()

    setupEvents: ->
        @$el.find('.view-json').unbind('click').click (event) ->
            utils.viewJSON (utils.getAcrossCollections [app.collections.tasksActive, app.collections.tasksScheduled], $(event.target).data('task-id'))?.toJSON()

module.exports = TasksView