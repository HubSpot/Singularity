View = require './view'

Request = require '../models/Request'

TasksActive = require '../collections/TasksActive'
TasksScheduled = require '../collections/TasksScheduled'
TasksCleaning = require '../collections/TasksCleaning'

class TasksView extends View

    isSorted: false

    templateBase:  require '../templates/tasksTable/tasksBase'

    # Figure out which template we'll use for the table based on the filter
    bodyTemplateMap:
        active:    require '../templates/tasksTable/tasksActiveBody'
        scheduled: require '../templates/tasksTable/tasksScheduledBody'
        cleaning:  require '../templates/tasksTable/tasksCleaningBody'

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

    initialize: ({@state, @searchFilter}) ->
        @bodyTemplate = @bodyTemplateMap[@state]

        @listenTo @collection, 'sync',   @render
        @listenTo @collection, 'remove', @render

        @searchChange = _.debounce @searchChange, 200

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
            tasksFilter: @state
            searchFilter: @searchFilter
            collectionSynced: @collection.synced
            haveTasks: @collection.length and @collection.synced

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
        utils.viewJSON 'task', $(e.target).data('task-id')

    removeTask: (e) ->
        $row = $(e.target).parents 'tr'
        id = $row.data 'task-id' 

        @collection.get(id).promptKill =>
            $row.remove()

    runTask: (e) =>
        $row = $(e.target).parents 'tr'
        id = $row.data 'task-id' 

        @collection.get(id).promptRun =>
            @collection.remove(taskModel)
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