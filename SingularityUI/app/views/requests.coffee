View = require './view'
AutoTailer = require './AutoTailer'
Request = require '../models/Request'

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
        activeDeploy: require '../templates/requestsTable/requestsAllBody'
        noDeploy: require '../templates/requestsTable/requestsAllBody'

    quartzTemplate: require '../templates/vex/quartzInfo'

    # Which table views have sub-filters (daemon, scheduled, on-demand)
    haveSubfilter: ['all', 'active', 'paused', 'cooldown', 'activeDeploy', 'noDeploy']

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
            searchFilter = @searchFilter.toLowerCase().split("@")[0]
            fuse = new Fuse(
                requests
                keys: ["request.id", "requestDeployState.activeDeploy.user", "request.owners"]
                threshold: 0.4
                maxPatternLength: 128)
            requests = fuse.search(searchFilter).reverse()

        # Only show requests that match the clicky filters
        if @state in @haveSubfilter and @subFilter isnt 'all'
            requests = _.filter requests, (request) =>
                filter = false

                if @subFilter.indexOf('SERVICE') isnt -1
                    filter = filter or request.type == 'SERVICE'
                if @subFilter.indexOf('WORKER') isnt -1
                    filter = filter or request.type == 'WORKER'
                if @subFilter.indexOf('SCHEDULED') isnt -1
                    filter = filter or request.type == 'SCHEDULED'
                if @subFilter.indexOf('ON_DEMAND') isnt -1
                    filter = filter or request.type == 'ON_DEMAND'
                if @subFilter.indexOf('RUN_ONCE') isnt -1
                    filter = filter or request.type == 'RUN_ONCE'

                filter

        # Filter by deploy type if applicable
        if @state in ['activeDeploy', 'noDeploy']
            requests = _.filter requests, (request) =>
                if @state == 'activeDeploy'
                    return !!request.requestDeployState?.activeDeploy
                else if @state == 'noDeploy'
                    return !request.requestDeployState?.activeDeploy
                return true

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

        if @state in ['all', 'active', 'activeDeploy', 'noDeploy']
            for request in requests
                request.starred = @collection.isStarred request.id

        @currentRequests = requests

    render: =>
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
        @afterRender()

    afterRender: =>
        super

        @renderTable()
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

    # Prepares the staged rendering and triggers the first one
    renderTable: =>
        return if not @$('table').length

        @$('table').show()
        @$('.empty-table-message').remove()
        @$('input[type="search"]').removeAttr('disabled').attr('placeholder','Filter requests').focus()

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

        request = new Request id: id

        request.promptRun (data) =>

            # If user wants to redirect to a file after the task starts
            if data.autoTail is 'on'
                autoTailer = new AutoTailer({
                    requestId: id
                    autoTailFilename: data.filename
                    autoTailTimestamp: +new Date()
                })

                autoTailer.startAutoTailPolling()

            else
                $row.addClass 'flash'
                setTimeout (=> $row.removeClass 'flash'), 500

                @trigger 'refreshrequest'
                setTimeout ( => @trigger 'refreshrequest'), 2500


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
            currentFilter = if @subFilter is 'all' then 'SERVICE-WORKER-SCHEDULED-ON_DEMAND-RUN_ONCE' else  @subFilter

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
        event.stopPropogation()

module.exports = RequestsView
