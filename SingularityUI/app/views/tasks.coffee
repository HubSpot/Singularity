View = require './view'

class TasksView extends View

    templateTasksActive: require './templates/tasksActive'
    templateTasksActiveTable: require './templates/tasksActiveTable'

    templateTasksScheduled: require './templates/tasksScheduled'
    templateTasksScheduledTable: require './templates/tasksScheduledTable'

    templateTasksCleaning: require './templates/tasksCleaning'
    templateTasksCleaningTable: require './templates/tasksCleaningTable'

    killTaskTemplate: require './templates/vex/killTask'

    initialize: ->
        @lastTasksFilter = @options.tasksFilter

    fetch: ->
        if @lastTasksFilter is 'active'
            @collection = app.collections.tasksActive

        if @lastTasksFilter is 'scheduled'
            @collection = app.collections.tasksScheduled

        if @lastTasksFilter is 'cleaning'
            @collection = app.collections.tasksCleaning

        @collection.fetch()

    refresh: ->
        return @ if @$el.find('input[type="search"]').val() isnt '' or @$el.find('[data-sorted-direction]').length

        @fetch(@lastTasksFilter).done =>
            @render(@lastTasksFilter, refresh = true)

        @

    render: (tasksFilter, refresh) ->
        forceFullRender = tasksFilter isnt @lastTasksFilter
        @lastTasksFilter = tasksFilter

        if @lastTasksFilter is 'active'
            @collection = app.collections.tasksActive
            template = @templateTasksActive
            templateTable = @templateTasksActiveTable

        if @lastTasksFilter is 'scheduled'
            @collection = app.collections.tasksScheduled
            template = @templateTasksScheduled
            templateTable = @templateTasksScheduledTable

        if @lastTasksFilter is 'cleaning'
            @collection = app.collections.tasksCleaning
            template = @templateTasksCleaning
            templateTable = @templateTasksCleaningTable

        @refresh() if not @collection.synced

        tasks = _.pluck @collection.sort().models, 'attributes'

        if @lastTasksFilter is 'active'
            tasks = tasks.reverse()

        context =
            collectionSynced: @collection.synced
            tasks: tasks

        partials =
            partials:
                tasksTable: templateTable

        searchWasFocused = @$el.find('input[type="search"]').is(':focus')

        $tasksTableContainer =  @$el.find('[data-tasks-table-container]')

        if not $tasksTableContainer.length or forceFullRender
            @$el.html template(context, partials)
        else
            $tasksTableContainer.html templateTable context

        @setupEvents()
        @setUpSearchEvents(refresh, searchWasFocused)
        utils.setupSortableTables()

        @

    setupEvents: ->
        @$el.find('[data-action="viewJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'task', $(e.target).data('task-id')

        $removeLinks = @$el.find('[data-action="remove"]')

        $removeLinks.unbind('click').on 'click', (e) =>
            $row = $(e.target).parents('tr')
            taskModel = @collection.get($(e.target).data('task-id'))

            vex.dialog.confirm
                message: @killTaskTemplate(taskId: taskModel.get('id'))
                callback: (confirmed) =>
                    return unless confirmed
                    taskModel.destroy()
                    delete app.allTasks[taskModel.get('id')] # TODO - move to model on destroy?
                    @collection.remove(taskModel)
                    $row.remove()

        $runNowLinks = @$el.find('[data-action="run-now"]')

        $runNowLinks.unbind('click').on 'click', (e) =>
            taskModel = @collection.get($(e.target).data('task-id'))
            $row = $(e.target).parents('tr')

            vex.dialog.confirm
                message: "<p>Are you sure you want to run this task immediately:</p><pre>#{ taskModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    taskModel.run()
                    @collection.remove(taskModel)
                    app.collections.tasksActive.fetch()
                    $row.remove()

    setUpSearchEvents: (refresh, searchWasFocused) ->
        $search = @$el.find('input[type="search"]')

        if not app.isMobile and (not refresh or searchWasFocused)
            setTimeout -> $search.focus()

        $rows = @$el.find('tbody > tr')

        lastText = _.trim $search.val()

        $search.unbind().on 'change keypress paste focus textInput input click keydown', =>
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