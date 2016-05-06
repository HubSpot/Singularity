View = require './view'

Request = require '../models/Request'
Slaves = require '../collections/Slaves'
fuzzy = require 'fuzzy'
micromatch = require 'micromatch'

killTemplate = require '../templates/vex/taskKill'

Utils = require '../utils'

vex = require 'vex'

class TasksView extends View

    isSorted: false

    templateBase:  require '../templates/tasksTable/tasksBase'

    templateRequestFilter: require '../templates/requestTypeFilter'

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

            'click [data-filter]': 'changeRequestFilters'

    initialize: ({@state, @requestsSubFilter, @searchFilter, @cleaningTasks, @taskKillRecords}) ->
        @bodyTemplate = @bodyTemplateMap[@state]

        @listenTo @collection, 'sync', @render
        @listenTo @cleaningTasks, 'change', @render
        @listenTo @taskKillRecords, 'change', @render

        @fuzzySearch = _.memoize(@fuzzySearch)

    fuzzySearch: (filter, tasks) =>
        host =
            extract: (o) ->
                "#{o.host}"
        id =
            extract: (o) ->
                "#{o.id}"
        unless Utils.isGlobFilter filter
            res1 = fuzzy.filter(filter, tasks, host)
            res2 = fuzzy.filter(filter, tasks, id)
        else
            res1 = tasks.filter (task) =>
                micromatch.any host.extract(task), filter
            res2 = tasks.filter (task) =>
                micromatch.any id.extract(task), filter
            res1 = fuzzy.filter('', res1, host) #Hack to make the object a fuzzy
            res2 = fuzzy.filter('', res2, id) #Hack to make the object a fuzzy
        _.uniq(_.pluck(_.sortBy(_.union(res1, res2), (t) => Utils.fuzzyAdjustScore(filter, t)), 'original').reverse())

    # Returns the array of tasks that need to be rendered
    filterCollection: =>
        tasks = _.pluck @collection.models, "attributes"

        # Only show tasks that match the search query
        if @searchFilter
            tasks = @fuzzySearch(@searchFilter, tasks)

        # Only show tasks of requests that match the clicky filters
        if @requestsSubFilter isnt 'all'
            tasks = _.filter tasks, (task) =>
                filter = false

                if @requestsSubFilter.indexOf('SERVICE') isnt -1
                    filter = filter or task.taskRequest.request.requestType == 'SERVICE'
                if @requestsSubFilter.indexOf('WORKER') isnt -1
                    filter = filter or task.taskRequest.request.requestType == 'WORKER'
                if @requestsSubFilter.indexOf('SCHEDULED') isnt -1
                    filter = filter or task.taskRequest.request.requestType == 'SCHEDULED'
                if @requestsSubFilter.indexOf('ON_DEMAND') isnt -1
                    filter = filter or task.taskRequest.request.requestType == 'ON_DEMAND'
                if @requestsSubFilter.indexOf('RUN_ONCE') isnt -1
                    filter = filter or task.taskRequest.request.requestType == 'RUN_ONCE'

                filter

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
        else if not @searchFilter
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
            requestsSubFilter: @requestsSubFilter
            haveTasks: @collection.length and @collection.synced

        partials =
            partials:
                tasksBody: @bodyTemplate
                requestsFilter: @templateRequestFilter

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
            config: config

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
        app.router.navigate "/tasks/#{ @state }/#{ @requestsSubFilter }/#{ @searchFilter }", { replace: true }

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
            input: """
                <input name="wait-for-replacement-task" id="wait-for-replacement-task" type="checkbox" checked /> Wait for replacement task to start before killing this task
                <input name="message" type="text" placeholder="Message (optional)" />
            """
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Kill task'
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            message: killTemplate id: id

            callback: (confirmed) =>
                return unless confirmed
                waitForReplacementTask = $('.vex #wait-for-replacement-task').is ':checked'
                deleteRequest = @collection.get(id).kill(confirmed.message, false, waitForReplacementTask)

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

    changeRequestFilters: (event) ->
        event.preventDefault()

        filter = $(event.currentTarget).data 'filter'

        if not event.metaKey
            # Select individual filters
            @requestsSubFilter = filter
        else
            # Select multiple filters
            currentFilter = if @requestsSubFilter is 'all' then 'SERVICE-WORKER-SCHEDULED-ON_DEMAND-RUN_ONCE' else  @requestsSubFilter

            currentFilter = currentFilter.split '-'
            needToAdd = not _.contains currentFilter, filter

            if needToAdd
                currentFilter.push filter
            else
                currentFilter = _.without currentFilter, filter

            @requestsSubFilter = currentFilter.join '-'

        @updateUrl()
        @render()

module.exports = TasksView
