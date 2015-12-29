View = require './view'

Request = require '../models/Request'
Slaves = require '../collections/Slaves'

killTemplate = require '../templates/vex/taskKill'

class TasksView extends View

    isSorted: false

    templateBase:  require '../templates/tasksTable/tasksBase'

    # Figure out which template we'll use for the table based on the filter
    bodyTemplateMap:
        active:          require '../templates/tasksTable/tasksActiveBody'
        scheduled:       require '../templates/tasksTable/tasksScheduledBody'
        cleaning:        require '../templates/tasksTable/tasksCleaningBody'
        lbcleanup:      require '../templates/tasksTable/tasksLbCleaningBody'
        decommissioning: require '../templates/tasksTable/tasksDecommissioningBody'

    # For staged rendering
    renderProgress: 0
    renderAtOnce: 100
    # Cache for the task array we're currently rendering
    currentTasks: []

    events: =>
        _.extend super,
            'click [data-action="viewJSON"]': 'viewJson'
            'click [data-action="remove"]': 'removeTask'
            'click [data-action="run-now"]': 'runTask'

            'change input[type="search"]': 'searchChange'
            'keyup input[type="search"]': 'searchChange'
            'input input[type="search"]': 'searchChange'

            'click th[data-sort-attribute]': 'sortTable'

    initialize: ({@state, @searchFilter, @cleaningTasks, @taskKillRecords}) ->
        @bodyTemplate = @bodyTemplateMap[@state]

        @listenTo @collection, 'sync', @render
        @listenTo @cleaningTasks, 'change', @render
        @listenTo @taskKillRecords, 'change', @render

        @searchChange = _.debounce @searchChange, 200

    # Returns the array of tasks that need to be rendered
    filterCollection: =>
        tasks = _.pluck @collection.models, "attributes"

        # Only show tasks that match the search query
        if @searchFilter
            tasks = _.filter tasks, (task) =>
                searchField = "#{ task.id }#{ task.host }".toLowerCase().replace(/-/g, '_')
                searchField.toLowerCase().indexOf(@searchFilter.toLowerCase().replace(/-/g, '_')) isnt -1
        # Sort the table if the user clicked on the table heading things
        if @sortAttribute?
            tasks = _.sortBy tasks, (task) =>
                # Traverse through the properties to find what we're after
                attributes = @sortAttribute.split '.'
                value = task

                for attribute in attributes
                    value = value[attribute]
                    return null if not value?

                return value
            if not @sortAscending
                tasks = tasks.reverse()
        else
            tasks.reverse() unless @state is 'scheduled'

        @currentTasks = tasks

    render: =>
        # Save the state of the caret if the search box has already been rendered
        $searchInput = $('.big-search-box')
        @prevSelectionStart = $searchInput[0].selectionStart
        @prevSelectionEnd = $searchInput[0].selectionEnd

        # Renders the base template
        # The table contents are rendered bit by bit as the user scrolls down.
        context =
            tasksFilter: @state
            searchFilter: @searchFilter
            collectionSynced: @collection.synced
            haveTasks: @collection.length and @collection.synced

        partials =
            partials:
                tasksBody: @bodyTemplate

        @$el.html @templateBase context, partials

        @renderTable()

        super.afterRender()

        # Reset search box caret
        $searchInput = $('.big-search-box')
        $searchInput[0].setSelectionRange(@prevSelectionStart, @prevSelectionEnd)

    # Prepares the staged rendering and triggers the first one
    renderTable: =>
        return if not @$('table').length

        @$('table').show()
        @$('.empty-table-message').remove()
        @$('input[type="search"]').removeAttr('disabled').attr('placeholder','Filter tasks').focus()

        $(window).scrollTop 0
        @filterCollection()
        @renderProgress = 0

        if not @currentTasks.length
            @$('table').hide()
            @$el.append '<div class="empty-table-message">No tasks that match your query</div>'
            return

        @renderTableChunk()

        $(window).on "scroll", @handleScroll

    renderTableChunk: =>
        if @ isnt app.views.current
            return

        firstStage = @renderProgress is 0

        newProgress = @renderAtOnce + @renderProgress
        tasks = @currentTasks.slice(@renderProgress, newProgress)
        @renderProgress = newProgress

        decomTasks = _.union(_.pluck(_.map(@cleaningTasks.where(cleanupType: 'DECOMISSIONING'), (t) -> t.toJSON()), 'taskId'), _.pluck(_.map(@taskKillRecords.where(taskCleanupType: 'DECOMISSIONING'), (t) -> t.toJSON()), 'taskId'))
        $contents = @bodyTemplate
            tasks: tasks
            rowsOnly: true
            decomissioning_tasks: decomTasks

        $table = @$ ".table-staged table"
        $tableBody = $table.find "tbody"

        if firstStage
            # Render the first batch
            $tableBody.html $contents
            # After the first stage of rendering we want to fix
            # the width of the columns to prevent having to recalculate
            # it constantly
            utils.fixTableColumns $table
        else
            $tableBody.append $contents

        @$('.actions-column a[title]').tooltip()

    sortTable: (event) =>
        @isSorted = true

        $target = $ event.currentTarget
        newSortAttribute = $target.attr "data-sort-attribute"

        $currentlySortedHeading = @$ "[data-sorted=true]"
        $currentlySortedHeading.removeAttr "data-sorted"
        $currentlySortedHeading.find('span').remove()

        if newSortAttribute is @sortAttribute and @sortAscending?
            @sortAscending = not @sortAscending
        else
            # timestamp should be DESC by default
            @sortAscending = if newSortAttribute is "startedAt" then false else true

        @sortAttribute = newSortAttribute

        $target.attr "data-sorted", "true"
        $target.append "<span class='glyphicon glyphicon-chevron-#{ if @sortAscending then 'up' else 'down' }'></span>"

        @renderTable()

    handleScroll: =>
        if @renderProgress >= @collection.length
            $(window).off "scroll"
            return

        if @animationFrameRequest?
            window.cancelAnimationFrame @animationFrameRequest

        @animationFrameRequest = window.requestAnimationFrame =>
            $table = @$ "tbody"
            tableBottom = $table.height() + $table.offset().top
            $window = $(window)
            scrollBottom = $window.scrollTop() + $window.height()
            if scrollBottom >= tableBottom
                @renderTableChunk()

    updateUrl: =>
        app.router.navigate "/tasks/#{ @state }/#{ @searchFilter }", { replace: true }

    viewJson: (e) ->
        task =
            taskId: $(e.target).data 'task-id'
            requestId: $(e.target).data 'request-id'
            nextRunAt: $(e.target).data 'nextrunat'

        # need to make a fetch for scheduled tasks
        if task.nextRunAt
            @trigger 'getPendingTask', task
        else
            utils.viewJSON @collection.get task.taskId

    removeTask: (e) ->
        $row = $(e.target).parents 'tr'
        id = $row.data 'task-id'

        @promptKill id, ->
            $row.remove()

    promptKill: (id, callback) ->
        vex.dialog.confirm
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Kill task'
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            message: killTemplate id: id

            callback: (confirmed) =>
                return unless confirmed
                deleteRequest = @collection.get(id).kill()

                # ignore errors (probably means you tried
                # to kill an already dead task)
                deleteRequest.error =>
                    app.caughtError()

                callback?()


    runTask: (e) =>
        $row = $(e.target).parents 'tr'
        id = $row.data 'task-id'

        model = @collection.get(id)
        model.promptRun =>
            $row.remove()

    searchChange: (event) =>
        return unless @ is app.views.current

        previousSearchFilter = @searchFilter
        $search = @$ "input[type='search']"
        @searchFilter = $search.val()

        if @searchFilter isnt previousSearchFilter
            @updateUrl()
            @renderTable()

module.exports = TasksView
