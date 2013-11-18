View = require './view'

class TasksView extends View

    template: require './templates/tasks'

    render: =>
        context =
            tasksActive: app.collections.tasksActive.sort().toJSON().reverse()
            tasksScheduled: app.collections.tasksScheduled.sort().toJSON().reverse()

        @$el.html @template context

        @setupEvents()
        @setUpSearchEvents()

    setupEvents: ->
        @$el.find('.view-json').unbind('click').click (event) ->
            utils.viewJSON (utils.getAcrossCollections ['tasksActive', 'tasksScheduled'], $(event.target).data('task-id'))?.toJSON()

    setUpSearchEvents: =>
        $search = @$el.find('input[type="search"]').focus()
        $rows = @$el.find('tbody > tr')

        lastText = _.trim $search.val()

        $search.on 'change keypress paste focus textInput input click keydown', =>
            text = _.trim $search.val()

            if text is ''
                $rows.removeClass('filtered')

            if text isnt lastText
                $rows.each ->
                    $row = $(@)

                    if not _.string.contains $row.data('task-id').toLowerCase(), text.toLowerCase()
                        $row.addClass('filtered')
                    else
                        $row.removeClass('filtered')

module.exports = TasksView