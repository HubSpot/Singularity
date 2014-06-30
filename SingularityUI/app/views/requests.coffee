View = require './view'

Request = require '../models/Request'

class RequestsView extends View

    templateBase:   require './templates/requestsBase'
    templateFilter: require './templates/requestsFilter'

    # Figure out which template we'll use for the table based on the filter
    bodyTemplateMap:
        all:      require './templates/requestsAllBody'
        active:   require './templates/requestsActiveBody'
        cooldown: require './templates/requestsCooldownBody'
        paused:   require './templates/requestsPausedBody'
        pending:  require './templates/requestsPendingBody'
        cleaning: require './templates/requestsCleaningBody'

    removeRequestTemplate: require './templates/vex/removeRequest'

    # Which table views have sub-filters (daemon, scheduled, on-demand)
    haveSubfilter: ['all', 'active', 'paused', 'cooldown']

    # For staged rendering
    renderProgress: 0
    renderAtOnce: 100
    # Cache for the request array we're currently rendering
    currentRequests: []

    events: =>
        _.extend super,
            'click [data-action="viewJSON"]': 'viewJson'
            'click [data-action="remove"]': 'removeRequest'
            'click [data-action="unpause"]': 'unpauseRequest'
            'click [data-action="starToggle"]': 'toggleStar'
            'click [data-action="run-now"]': 'runRequest'
            'click [data-requests-active-filter]': 'changeFilters'

            'change input[type="search"]': 'searchChange'
            'keyup input[type="search"]': 'searchChange'
            'input input[type="search"]': 'searchChange'

            'click th[data-sort-attribute]': 'sortTable'

    initialize: ({@requestsFilter, @requestsSubFilter, @searchFilter}) ->
        @bodyTemplate = @bodyTemplateMap[@requestsFilter]

        $(window).on "scroll", @handleScroll

        # Set up collection
        collectionMap =
            all:      app.collections.requestsAll
            active:   app.collections.requestsActive
            cooldown: app.collections.requestsCooldown
            paused:   app.collections.requestsPaused
            pending:  app.collections.requestsPending
            cleaning: app.collections.requestsCleaning

        @collectionSynced = false
        @collection = collectionMap[@requestsFilter]
        # Initial fetch
        @collection.fetch().done =>
            @collectionSynced = true
            @render()

    # Called by app on active view
    refresh: ->
        return @ if @$el.find('[data-sorted-direction]').length
        # Don't refresh if user is scrolled down, viewing the table (arbitrary value)
        return @ if $(window).scrollTop() > 200
        @collection.fetch().done =>
            @renderTable()

    # Returns the array of requests that need to be rendered
    filterCollection: =>
        requests = _.pluck @collection.models, "attributes"

        # Only show requests that match the search query
        if @searchFilter
            requests = _.filter requests, (request) =>
                request.name.toLowerCase().indexOf(@searchFilter.toLowerCase()) isnt -1
        
        # Only show requests that match the clicky filters
        if @requestsFilter in @haveSubfilter and @requestsSubFilter isnt 'all'
            requests = _.filter requests, (request) =>
                filter = false

                if @requestsSubFilter.indexOf('daemon') isnt -1
                    filter = filter or request.daemon
                if @requestsSubFilter.indexOf('scheduled') isnt -1
                    filter = filter or request.scheduled
                if @requestsSubFilter.indexOf('on-demand') isnt -1
                    filter = filter or request.onDemand
                filter

        # Sort the table if the user clicked on the table heading things
        if @sortAttribute?
            requests = _.sortBy requests, @sortAttribute
            if not @sortAscending
                requests = requests.reverse()
        else
            requests.reverse()
            
        @currentRequests = requests

    render: =>
        ###
        Renders the base template

        The table contents are rendered bit by bit as the user scrolls down.
        ###
        context =
            requestsFilter: @requestsFilter
            requestsSubFilter: @requestsSubFilter
            searchFilter: @searchFilter
            hasSubFilter: @requestsFilter in @haveSubfilter
            collectionSynced: @collectionSynced
            haveRequests: @collection.length and @collectionSynced

        partials = 
            partials:
                requestsBody: @bodyTemplate

        if @requestsFilter in @haveSubfilter
            partials.partials.requestsFilter = @templateFilter

        @$el.html @templateBase context, partials

        @renderTable()

        @

    # Prepares the staged rendering and triggers the first one
    renderTable: =>
        $(window).scrollTop 0
        @filterCollection()
        @renderProgress = 0

        @renderTableChunk()

    renderTableChunk: =>
        if @ isnt app.views.current
            return

        firstStage = @renderProgress is 0

        newProgress = @renderAtOnce + @renderProgress
        requests = @currentRequests.slice(@renderProgress, newProgress)
        @renderProgress = newProgress

        $contents = @bodyTemplate
            requests: requests
            rowsOnly: true
        
        $table = @$ "tbody"
        $headings = @$ "thead th"
        if firstStage and $headings.length > 0 and not @fixedColumns
            # After the first stage of rendering we want to fix
            # the width of the columns to prevent having to recalculate
            # it constantly

            # Reset any previous widths
            $table.parent().css "table-layout", "auto"
            $headings.css "width", "auto"

            # Render the first batch
            $table.html $contents

            # Set a %-width to each table header heading based on current values
            totalWidth = $table.width()
            for $heading in $headings
                $heading = $ $heading
                percentage = $heading.width() / totalWidth * 100
                $heading.css "width", "#{ percentage }%"

            # Set the table layout to be fixed based on these new widths
            $table.parent().css "table-layout", "fixed"
            @fixedColumns = true
        else if firstStage
            $table.html $contents
        else
            $table.append $contents

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
            @sortAscending = if newSortAttribute is "timestamp" then false else true

        @sortAttribute = newSortAttribute

        $target.attr "data-sorted", "true"
        $target.attr "data-sorted-direction", if @sortAscending then "ascending" else "descending"

        @renderTable()

    handleScroll: (event) =>
        $table = @$ "tbody"
        tableBottom = $table.height() + $table.offset().top
        $window = $(window)
        scrollBottom = $window.scrollTop() + $window.height()
        if scrollBottom >= tableBottom
            @renderTableChunk()

    updateUrl: =>
        app.router.navigate "/requests/#{ @requestsFilter }/#{ @requestsSubFilter }/#{ @searchFilter }", { replace: true }

    viewJson: (e) ->
        utils.viewJSON 'request', $(e.target).data('request-id')

    removeRequest: (e) ->
        $row = $(e.target).parents('tr')
        requestModel = @collection.get($(e.target).data('request-id'))

        if $(e.target).data('action-remove-type') is 'deletePaused'
            vex.dialog.confirm
                message: "<p>Are you sure you want to delete the paused request?</p><pre>#{ requestModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    $row.remove()
                    requestModel.deletePaused().done =>
                        delete app.allRequests[requestModel.get('id')]
                        @collection.remove(requestModel)

        else
            vex.dialog.confirm
                message: @removeRequestTemplate(requestId: requestModel.get('id'))
                buttons: [
                    $.extend({}, vex.dialog.buttons.YES, (text: 'Remove', className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'))
                    vex.dialog.buttons.NO
                ]
                callback: (confirmed) =>
                    return unless confirmed
                    requestModel.destroy()
                    delete app.allRequests[requestModel.get('id')] # TODO - move to model on destroy?
                    @collection.remove(requestModel)
                    $row.remove()

    unpauseRequest: (e) ->
        $row = $(e.target).parents('tr')
        requestModel = @collection.get($(e.target).data('request-id'))

        vex.dialog.confirm
            message: "<p>Are you sure you want to unpause the request?</p><pre>#{ requestModel.get('id') }</pre>"
            callback: (confirmed) =>
                return unless confirmed
                
                if @requestsFilter is "paused"
                    $row.remove()
                    
                requestModel.unpause().done =>
                    @refresh()
                        
    toggleStar: (e) ->
        $target = $(e.target)
        $table = $target.parents 'table'

        requestName = $target.data 'request-name'
        starred = $target.attr('data-starred') is 'true'

        app.collections.requestsStarred.toggle(requestName)
        $requests = $table.find("""[data-request-name="#{ requestName }"]""")

        if starred
            $requests.each -> $(@).attr('data-starred', 'false')
        else
            $requests.each -> $(@).attr('data-starred', 'true')

    runRequest: (e) ->
        requestModel = new Request id: $(e.target).data('request-id')
        $row = $(e.target).parents 'tr'

        requestType = $(e.target).data 'request-type'

        dialogOptions =
            message: "<p>Are you sure you want to run a task for this #{ requestType } request immediately?</p><pre>#{ requestModel.get('id') }</pre>"
            buttons: [
                $.extend({}, vex.dialog.buttons.YES, text: 'Run now')
                vex.dialog.buttons.NO
            ]
            callback: (confirmedOrPromptData) =>
                return if confirmedOrPromptData is false

                requestModel.run(confirmedOrPromptData)
                utils.flashRow $row

        dialogType = vex.dialog.prompt
        dialogOptions.message += '<p>Additional command line input (optional):</p>'

        dialogType dialogOptions

    changeFilters: (e) ->
        e.preventDefault()
        @requestsSubFilter = $(e.target).data('requests-active-filter')
        if e.metaKey or e.ctrlKey or e.shiftKey
            @requestsSubFilter = $(e.target).data('requests-active-filter-shift')

        @updateUrl()
        @render()

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

module.exports = RequestsView
