View = require './view'

Request = require '../models/Request'

class TasksView extends View

    templateTasksActive: require './templates/tasksActive'
    templateTasksActiveTable: require './templates/tasksActiveTable'

    templateTasksScheduled: require './templates/tasksScheduled'
    templateTasksScheduledTable: require './templates/tasksScheduledTable'

    templateTasksCleaning: require './templates/tasksCleaning'
    templateTasksCleaningTable: require './templates/tasksCleaningTable'

    killTaskTemplate: require './templates/vex/killTask'

    events:
        'click [data-action="viewJSON"]': 'viewJson'
        'click [data-action="remove"]': 'removeTask'
        'click [data-action="run-now"]': 'runTask'

        'change input[type="search"]': 'searchChange'
        'keyup input[type="search"]': 'searchChange'
        'input input[type="search"]': 'searchChange'

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
        return @ if @$el.find('[data-sorted-direction]').length

        @fetch(@lastTasksFilter).done =>
            @render(@lastTasksFilter, @lastSearchFilter, refresh = true)

        @

    render: (tasksFilter, searchFilter, refresh) ->
        forceFullRender = tasksFilter isnt @lastTasksFilter
        @lastTasksFilter = tasksFilter
        @lastSearchFilter = searchFilter
        @$el.find('input[type="search"]').val searchFilter

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
            searchFilter: searchFilter

        partials =
            partials:
                tasksTable: templateTable

        $tasksTableContainer =  @$el.find('[data-tasks-table-container]')

        if not $tasksTableContainer.length or forceFullRender
            @$el.html template(context, partials)

        else
            $tasksTableContainer.html templateTable context
        utils.setupSortableTables()

        @

    viewJson: (e) ->
        utils.viewJSON 'task', $(e.target).data('task-id')

    removeTask: (e) ->
        $row = $(e.target).parents('tr')
        taskModel = @collection.get($(e.target).data('task-id'))

        vex.dialog.confirm
            buttons: [
                $.extend({}, vex.dialog.buttons.YES, (text: 'Kill task', className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'))
                vex.dialog.buttons.NO
            ]
            message: @killTaskTemplate(taskId: taskModel.get('id'))
            callback: (confirmed) =>
                return unless confirmed
                taskModel.destroy()
                delete app.allTasks[taskModel.get('id')] # TODO - move to model on destroy?
                @collection.remove(taskModel)
                $row.remove()

    runTask: (e) =>
        taskModel = @collection.get($(e.target).data('task-id'))
        $row = $(e.target).parents('tr')

        requestModel = new Request id: taskModel.get "requestId"
        requestModel.promptRun =>
            @collection.remove(taskModel)
            app.collections.tasksActive.fetch()
            $row.remove()

    searchChange: ->
        onChange = =>
            return unless @ is app.views.current
            $search = @$el.find('input[type="search"]')
            $rows = @$el.find('tbody > tr')
            lastText = ''

            searchText = _.trim $search.val()

            if searchText is ''
                $rows.removeClass('filtered')
                app.router.navigate "/tasks/#{ @lastTasksFilter }", { replace: true }

            if searchText isnt lastText
                @lastSearchFilter = searchText
                app.router.navigate "/tasks/#{ @lastTasksFilter }/#{ @lastSearchFilter }", { replace: true }

                $rows.each ->
                    $row = $(@)

                    rowText = $row.data('task-id')
                    host = $row.data('task-host')
                    rowText = "#{ rowText } #{ host }" if host?

                    if utils.matchLowercaseOrWordsInWords(searchText, rowText)
                        $row.removeClass('filtered')
                    else
                        $row.addClass('filtered')

            @$('table').each ->
                utils.handlePotentiallyEmptyFilteredTable $(@), 'task', searchText

        (_.debounce onChange, 200)()

module.exports = TasksView