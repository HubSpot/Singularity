View = require './view'

class TasksView extends View

    templateBase: require './templates/tasksBase'

    # Figure out which template we'll use for the table based on the filter
    bodyTemplateMap:
        active:    require './templates/tasksActiveBody'
        scheduled: require './templates/tasksScheduledBody'
        cleaning:  require './templates/tasksCleaningBody'

    killTaskTemplate: require './templates/vex/killTask'

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

    initialize: ({@tasksFilter, @searchFilter}) ->
        @bodyTemplate = @bodyTemplateMap[@tasksFilter]

        collectionMap =
            active: app.collections.tasksActive
            scheduled: app.collections.tasksScheduled
            cleaning: app.collections.tasksCleaning

        @collectionSynced = false
        @collection = collectionMap[@tasksFilter]

        @collection.fetch().done =>
            @collectionSynced = true
            @render()

    refresh: ->
        return @ if @$el.find('[data-sorted-direction]').length
        # Don't refresh if user is scrolled down, viewing the table (arbitrary value)
        return @ if $(window).scrollTop() > 200
        @collection.fetch().done =>
            @renderTable()

    # Returns the array of tasks that need to be rendered
    filterCollection: =>
        tasks = _.pluck @collection.models, "attributes"

        # Only show tasks that match the search query
        if @searchFilter
            tasks = _.filter tasks, (request) =>
                searchField = "#{ request.name }#{ request.host }"
                searchField.toLowerCase().indexOf(@searchFilter.toLowerCase()) isnt -1
        
        # Sort the table if the user clicked on the table heading things
        if @sortAttribute?
            tasks = _.sortBy tasks, @sortAttribute
            if not @sortAscending
                tasks = tasks.reverse()
        else
            tasks.reverse()
            
        @currentTasks = tasks

    render: ->
        # Renders the base template
        # The table contents are rendered bit by bit as the user scrolls down.
        context =
            tasksFilter: @tasksFilter
            searchFilter: @searchFilter
            collectionSynced: @collectionSynced
            haveTasks: @collection.length and @collectionSynced

        partials = 
            partials:
                tasksBody: @bodyTemplate

        @$el.html @templateBase context, partials

        @renderTable()

    # Prepares the staged rendering and triggers the first one
    renderTable: =>
        $(window).scrollTop 0
        @filterCollection()
        @renderProgress = 0

        @renderTableChunk()

        $(window).on "scroll", @handleScroll

    renderTableChunk: =>
        if @ isnt app.views.current
            return

        firstStage = @renderProgress is 0

        newProgress = @renderAtOnce + @renderProgress
        tasks = @currentTasks.slice(@renderProgress, newProgress)
        @renderProgress = newProgress

        $contents = @bodyTemplate
            tasks: tasks
            rowsOnly: true
        
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

    sortTable: (event) =>
        $target = $ event.currentTarget
        newSortAttribute = $target.attr "data-sort-attribute"

        $currentlySortedHeading = @$ "[data-sorted=true]"
        $currentlySortedHeading.removeAttr "data-sorted"
        $currentlySortedHeading.removeAttr "data-sorted-direction"

        if newSortAttribute is @sortAttribute and @sortAscending?
            @sortAscending = not @sortAscending
        else
            # timestamp should be DESC by default
            @sortAscending = if newSortAttribute is "startedAt" then false else true

        @sortAttribute = newSortAttribute

        $target.attr "data-sorted", "true"
        $target.attr "data-sorted-direction", if @sortAscending then "ascending" else "descending"

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
        app.router.navigate "/tasks/#{ @tasksFilter }/#{ @searchFilter }", { replace: true }

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

        vex.dialog.confirm
            message: "<p>Are you sure you want to run this task immediately?</p><pre>#{ taskModel.get('id') }</pre>"
            callback: (confirmed) =>
                return unless confirmed
                taskModel.run()
                @collection.remove(taskModel)
                app.collections.tasksActive.fetch()
                $row.remove()

    searchChange: (event) =>
        # Add a little delay to the event so we don't run it for every keystroke
        if @searchTimeout?
            clearTimeout @searchTimeout

        @searchTimeout = setTimeout @processSearchChange, 200

    processSearchChange: =>
        return unless @ is app.views.current

        previousSearchFilter = @searchFilter
        $search = @$ "input[type='search']"
        @searchFilter = _.trim $search.val()

        if @searchFilter isnt previousSearchFilter
            @updateUrl()
            @renderTable()

module.exports = TasksView