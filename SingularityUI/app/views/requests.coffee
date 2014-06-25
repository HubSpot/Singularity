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

    # Which table views have sub-filters (daemon, scheduled, on-demand)
    haveSubfilter: ['all', 'active', 'paused', 'cooldown']

    events:
        'click [data-action="viewJSON"]': 'viewJson'
        'click [data-action="remove"]': 'removeRequest'
        'click [data-action="unpause"]': 'unpauseRequest'
        'click [data-action="starToggle"]': 'toggleStar'
        'click [data-action="run-now"]': 'runRequest'
        'click [data-requests-active-filter]': 'changeFilters'

        'change input[type="search"]': 'searchChange'
        'keyup input[type="search"]': 'searchChange'
        'input input[type="search"]': 'searchChange'

    initialize: ({@requestsFilter, @requestsSubFilter, @searchFilter}) ->
        @bodyTemplate = @bodyTemplateMap[@requestsFilter]

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
        @collection.on "sync", =>
            @collectionSynced = true
            @renderTable()
        # Initial fetch
        @collection.fetch()

    # Called by app on active view
    refresh: ->
        return @ if @$el.find('[data-sorted-direction]').length
        @collection.fetch()

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

        requests.reverse()

    render: =>
        context =
            requestsFilter: @requestsFilter
            requestsSubFilter: @requestsSubFilter
            searchFilter: @searchFilter

            hasSubFilter: @requestsFilter in @haveSubfilter

            collectionSynced: @collectionSynced
            requests: @filterCollection()

        partials = 
            partials:
                requestsBody: @bodyTemplate

        if @requestsFilter in @haveSubfilter
            partials.partials.requestsFilter = @templateFilter

        @$el.html @templateBase context, partials

        utils.setupSortableTables()

        @

    renderTable: =>
        $table = @$ "[data-requests-body-container]"
        $table.html @bodyTemplate
            requests: @filterCollection()
            collectionSynced: @collectionSynced

    updateUrl: =>
        app.router.navigate "/requests/#{ @requestsFilter }/#{ @requestsSubFilter }/#{ @searchFilter }", { replace: true }

    viewJson: (e) ->
        utils.viewJSON 'request', $(e.target).data('request-id')

    removeRequest: (e) ->
        $row = $(e.target).parents('tr')
        requestModel = @collection.get($(e.target).data('request-id'))
        requestModel.promptRemove =>
            $row.remove()

    unpauseRequest: (e) ->
        $row = $(e.target).parents('tr')
        requestModel = @collection.get($(e.target).data('request-id'))
        requestModel.promptUnpause =>
            @refresh()

    runRequest: (e) ->
        $row = $(e.target).parents 'tr'

        requestModel = new Request id: $(e.target).data('request-id')
        requestModel.promptRun =>
            utils.flashRow $row
                        
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