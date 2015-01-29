View = require './view'

class RequestsView extends View

    isSorted: false

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

    quartzTemplate: require '../templates/vex/quartzInfo'

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
            'click [data-action="scale"]': 'scaleRequest'
            'click [data-action="unpause"]': 'unpauseRequest'
            'click [data-action="starToggle"]': 'toggleStar'
            'click [data-action="run-now"]': 'runRequest'
            'click [data-action="show-quartz"]': 'showQuartz'
            'click [data-filter]': 'changeFilters'

            'change input[type="search"]': 'searchChange'
            'keyup input[type="search"]': 'searchChange'
            'input input[type="search"]': 'searchChange'

            'click th[data-sort-attribute]': 'sortTable'

    initialize: ({@state, @subFilter, @searchFilter}) ->
        @bodyTemplate = @bodyTemplateMap[@state]
        @listenTo @collection, 'sync', @render

        # So we don't spam it with every keystroke
        @searchChange = _.debounce @searchChange, 200

    # Returns the array of requests that need to be rendered
    filterCollection: =>
        requests = _.pluck @collection.models, "attributes"

        # Only show requests that match the search query
        if @searchFilter
            requests = _.filter requests, (request) =>
                searchFilter = @searchFilter.toLowerCase().split("@")[0]
                valuesToSearch = []

                for user in request.request.owners ? []
                  valuesToSearch.push(user.split("@")[0])

                valuesToSearch.push(request.request.id)
                valuesToSearch.push(request.requestDeployState?.activeDeploy?.user)

                searchTarget = valuesToSearch.join("")
                searchTarget.toLowerCase().indexOf(searchFilter) isnt -1

        # Only show requests that match the clicky filters
        if @state in @haveSubfilter and @subFilter isnt 'all'
            requests = _.filter requests, (request) =>
                filter = false

                if @subFilter.indexOf('daemon') isnt -1
                    filter = filter or request.daemon
                if @subFilter.indexOf('scheduled') isnt -1
                    filter = filter or request.scheduled
                if @subFilter.indexOf('ondemand') isnt -1
                    filter = filter or request.onDemand
                filter

        # Sort the table if the user clicked on the table heading things
        if @sortAttribute?
            requests = _.sortBy requests, (request) =>
                # Traverse through the properties to find what we're after
                attributes = @sortAttribute.split '.'
                value = request

                for attribute in attributes
                    value = value[attribute]
                    return null if not value?

                return value

            if not @sortAscending
                requests = requests.reverse()
        else
            requests.reverse()

        if @state in ['all', 'active']
            for request in requests
                request.starred = @collection.isStarred request.id
            
        @currentRequests = requests

    preventSearchOverwrite: ->
        # If you've got a lot of requests like we do at HubSpot, the collection
        # behind this view will take a while to download & parse. If you type stuff
        # in the search field before this happens, it'll all be wiped.
        $searchBox = @$ 'input[type="search"]'
        searchVal = $searchBox.val()

        @searchFilter = searchVal if not @searchFilter

        if $searchBox.is ':focus'
            @focusSearchAfterRender = true

    render: =>
        @preventSearchOverwrite()
        
        # Renders the base template
        # The table contents are rendered bit by bit as the user scrolls down.
        context =
            config: config
            requestsFilter: @state
            requestsSubFilter: @subFilter
            searchFilter: @searchFilter
            hasSubFilter: @state in @haveSubfilter
            collectionSynced: @collection.synced
            haveRequests: @collection.length and @collection.synced

        partials = 
            partials:
                requestsBody: @bodyTemplate

        if @state in @haveSubfilter
            partials.partials.requestsFilter = @templateFilter

        @$el.html @templateBase context, partials
          
        if @focusSearchAfterRender
            $searchBox = @$ 'input[type="search"]'
            $searchBox.focus()
            $searchBox[0].setSelectionRange @searchFilter.length, @searchFilter.length
            @focusSearchAfterRender = false

        @renderTable()

    # Prepares the staged rendering and triggers the first one
    renderTable: =>
        return if not @$('table').length

        @$('table').show()
        @$('.empty-table-message').remove()

        $(window).scrollTop 0
        @filterCollection()
        @renderProgress = 0

        if not @currentRequests.length
            @$('table').hide()
            @$el.append '<div class="empty-table-message">No requests that match your query</div>'
            return

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
            requestsSubFilter: @subFilter
        
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
        @$('.schedule-header span#schedule').popover({
            animation: false,
            placement : 'auto',
            trigger: 'hover',
            delay: {hide: 400},
            container: 'span#schedule',
            html: true,
            content: "<p>All schedules are in <a data-action='show-quartz'>quartz format</a> unless otherwise specified.</p>",
            template: '<div class="popover table-header-popover" role="tooltip"><div class="popover-content"></div></div>'
        }).on
            show: (e) ->
                @showPopover(e)
            hide: (e) ->
                @hidePopover(e)

    hidePopover: (e) ->
        $this = $(this)
        if $this.data("forceHidePopover")
            $this.data "forceHidePopover", false
            return true
        e.stopImmediatePropagation()
        clearTimeout $this.data("popoverTO")
        $this.data "hoveringPopover", false
        $this.data "waitingForPopoverTO", true
        $this.data "popoverTO", setTimeout(->
            unless $this.data("hoveringPopover")
                $this.data "forceHidePopover", true
                $this.data "waitingForPopoverTO", false
                $this.popover "hide"
        , 1500)
        false

    showPopover: (e) ->
        $this = $(this)
        $this.data "hoveringPopover", true
        e.stopImmediatePropagation()  if $this.data("waitingForPopoverTO")

    sortTable: (event) =>
        @isSorted = true

        $target = $ event.currentTarget
        newSortAttribute = $target.attr "data-sort-attribute"

        $currentlySortedHeading = @$ "[data-sorted=true]"
        $currentlySortedHeading.removeAttr "data-sorted"
        $currentlySortedHeading.find("span.glyphicon-chevron-up").remove()
        $currentlySortedHeading.find("span.glyphicon-chevron-down").remove()

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
        app.router.navigate "/requests/#{ @state }/#{ @subFilter }/#{ @searchFilter }", { replace: true }

    viewJson: (e) ->
        id = $(e.target).parents('tr').data 'request-id'
        utils.viewJSON @collection.get id

    removeRequest: (e) ->
        $row = $(e.target).parents 'tr'
        id = $row.data('request-id')

        @collection.get(id).promptRemove =>
            $row.remove()

    unpauseRequest: (e) ->
        $row = $(e.target).parents 'tr'
        id = $row.data('request-id')

        @collection.get(id).promptUnpause =>
            $row.remove()
            @trigger 'refreshrequest'

    scaleRequest: (e) ->
        $row = $(e.target).parents 'tr'
        id = $row.data('request-id')

        @collection.get(id).promptScale =>
          $row.addClass 'flash'
          @trigger 'refreshrequest'

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

        @collection.toggleStar id

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
            @subFilter = filter
        else
            # Select multiple filters
            currentFilter = if @subFilter is 'all' then 'daemon-ondemand-scheduled' else  @subFilter

            currentFilter = currentFilter.split '-'
            needToAdd = not _.contains currentFilter, filter

            if needToAdd
                currentFilter.push filter
            else
                currentFilter = _.without currentFilter, filter

            @subFilter = currentFilter.join '-'

        @updateUrl()
        @render()

    searchChange: (event) =>
        return unless @ is app.views.current

        previousSearchFilter = @searchFilter
        $search = @$ "input[type='search']"
        @searchFilter = $search.val()

        if @searchFilter isnt previousSearchFilter
            @updateUrl()
            @renderTable()

    showQuartz: (event) =>
        vex.dialog.alert
            message: @quartzTemplate

module.exports = RequestsView
