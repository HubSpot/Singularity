View = require './view'

Request = require '../models/Request'

Requests = require '../collections/Requests'
RequestsPending = require '../collections/RequestsPending'
RequestsCleaning = require '../collections/RequestsCleaning'

RequestsStarred = require '../collections/RequestsStarred'

class RequestsView extends View

    templateBase:   require '../templates/requestsTable/requestsBase'
    templateFilter: require '../templates/requestsTable/requestsFilter'

    # Figure out which template we'll use for the table based on the filter
    bodyTemplateMap:
        all:      require '../templates/requestsTable/requestsAllBody'
        active:   require '../templates/requestsTable/requestsActiveBody'
        cooldown: require '../templates/requestsTable/requestsCooldownBody'
        paused:   require '../templates/requestsTable/requestsPausedBody'
        pending:  require '../templates/requestsTable/requestsPendingBody'
        cleaning: require '../templates/requestsTable/requestsCleaningBody'

    # Used to figure out which collection to use
    collectionMap:
        all:      Requests
        active:   Requests
        cooldown: Requests
        paused:   Requests
        pending:  RequestsPending
        cleaning: RequestsCleaning

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
            'click [data-filter]': 'changeFilters'

            'change input[type="search"]': 'searchChange'
            'keyup input[type="search"]': 'searchChange'
            'input input[type="search"]': 'searchChange'

            'click th[data-sort-attribute]': 'sortTable'

    initialize: ({@requestsFilter, @requestsSubFilter, @searchFilter}) ->
        @bodyTemplate = @bodyTemplateMap[@requestsFilter]

        # Set up collection
        @collectionSynced = false
        @collection = new @collectionMap[@requestsFilter] [], state: @requestsFilter
        # Initial fetch
        @collection.fetch().done =>
            @collectionSynced = true
            @render()
        @requestsStarred = new RequestsStarred
        @requestsStarred.fetch()

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
                searchTarget = "#{ request.name }#{ request.deployUser}"
                searchTarget.toLowerCase().indexOf(@searchFilter.toLowerCase()) isnt -1
        
        # Only show requests that match the clicky filters
        if @requestsFilter in @haveSubfilter and @requestsSubFilter isnt 'all'
            requests = _.filter requests, (request) =>
                filter = false

                if @requestsSubFilter.indexOf('daemon') isnt -1
                    filter = filter or request.daemon
                if @requestsSubFilter.indexOf('scheduled') isnt -1
                    filter = filter or request.scheduled
                if @requestsSubFilter.indexOf('ondemand') isnt -1
                    filter = filter or request.onDemand
                filter

        # Sort the table if the user clicked on the table heading things
        if @sortAttribute?
            requests = _.sortBy requests, @sortAttribute
            if not @sortAscending
                requests = requests.reverse()
        else
            requests.reverse()

        for request in requests
            request.starred = @requestsStarred.get(request.id)?
            
        @currentRequests = requests

    render: =>
        # Renders the base template
        # The table contents are rendered bit by bit as the user scrolls down.
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
        requests = @currentRequests.slice(@renderProgress, newProgress)
        @renderProgress = newProgress

        $contents = @bodyTemplate
            requests:          requests
            rowsOnly:          true
            requestsSubFilter: @requestsSubFilter
        
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
        $currentlySortedHeading.find('span').remove()

        if newSortAttribute is @sortAttribute and @sortAscending?
            @sortAscending = not @sortAscending
        else
            # timestamp should be DESC by default
            @sortAscending = if newSortAttribute is "timestamp" then false else true

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
        app.router.navigate "/requests/#{ @requestsFilter }/#{ @requestsSubFilter }/#{ @searchFilter }", { replace: true }

    viewJson: (e) ->
        utils.viewJSON 'request', $(e.target).data('request-id')

    removeRequest: (e) ->
        $row = $(e.target).parents 'tr'
        id = $row.data('request-id')

        @collection.get(id).promptRemove =>
            $row.remove()

    unpauseRequest: (e) ->
        $row = $(e.target).parents 'tr'
        id = $row.data('request-id')

        @collection.get(id).promptUnpause =>
            @refresh()

    runRequest: (e) ->
        $row = $(e.target).parents 'tr'
        id = $row.data('request-id')

        @collection.get(id).promptRun =>
            $row.addClass 'flash'
            setTimeout (=> $row.removeClass 'flash'), 500
                        
    toggleStar: (e) ->
        $target = $(e.currentTarget)
        $row = $target.parents 'tr'

        id = $row.data 'request-id'

        @requestsStarred.toggle id

        starred = $target.attr('data-starred') is "true"
        if starred
            $target.attr 'data-starred', 'false'
        else
            $target.attr 'data-starred', 'true'

    changeFilters: (event) ->
        event.preventDefault()

        filter = $(event.currentTarget).data 'filter'

        if not event.metaKey
            # Select individual filters
            @requestsSubFilter = filter
        else
            # Select multiple filters
            currentFilter = if @requestsSubFilter is 'all' then 'daemon-ondemand-scheduled' else  @requestsSubFilter

            currentFilter = currentFilter.split '-'
            needToAdd = not _.contains currentFilter, filter

            if needToAdd
                currentFilter.push filter
            else
                currentFilter = _.without currentFilter, filter

            @requestsSubFilter = currentFilter.join '-'

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
        @searchFilter = $search.val()

        if @searchFilter isnt previousSearchFilter
            @updateUrl()
            @renderTable()

module.exports = RequestsView
