View = require './view'

Request = require '../models/Request'

class RequestsView extends View

    templateRequestsActive: require './templates/requestsActive'
    templateRequestsActiveBody: require './templates/requestsActiveBody'
    templateRequestsActiveNav: require './templates/requestsActiveNav'

    templateRequestsPaused: require './templates/requestsPaused'
    templateRequestsPausedBody: require './templates/requestsPausedBody'

    templateRequestsPending: require './templates/requestsPending'
    templateRequestsPendingBody: require './templates/requestsPendingBody'

    templateRequestsCleaning: require './templates/requestsCleaning'
    templateRequestsCleaningBody: require './templates/requestsCleaningBody'

    removeRequestTemplate: require './templates/vex/removeRequest'

    initialize: ->
        @lastRequestsFilter = @options.requestsFilter
        @lastRequestsSubFilter = @options.requestsSubFilter

    fetch: ->
        @collection = switch @lastRequestsFilter
            when 'active'
                app.collections.requestsActive
            when 'paused'
                app.collections.requestsPaused
            when 'pending'
                app.collections.requestsPending
            when 'cleaning'
                app.collections.requestsCleaning

        @collection.fetch()

    refresh: ->
        return @ if @$el.find('[data-sorted-direction]').length

        @fetch(@lastRequestsFilter).done =>
            @render(@lastRequestsFilter, @lastRequestsSubFilter, @lastSearchFilter, refresh = true)

        @

    render: (requestsFilter, requestsSubFilter, searchFilter, refresh) =>
        forceFullRender = requestsFilter isnt @lastRequestsFilter

        @lastRequestsFilter = requestsFilter
        @lastRequestsSubFilter = requestsSubFilter
        @lastSearchFilter = searchFilter

        if @lastRequestsFilter is 'active'
            @collection = app.collections.requestsActive
            template = @templateRequestsActive
            templateBody = @templateRequestsActiveBody
            templateNav = @templateRequestsActiveNav

        if @lastRequestsFilter is 'paused'
            @collection = app.collections.requestsPaused
            template = @templateRequestsPaused
            templateBody = @templateRequestsPausedBody

        if @lastRequestsFilter is 'pending'
            @collection = app.collections.requestsPending
            template = @templateRequestsPending
            templateBody = @templateRequestsPendingBody

        if @lastRequestsFilter is 'cleaning'
            @collection = app.collections.requestsCleaning
            template = @templateRequestsCleaning
            templateBody = @templateRequestsCleaningBody

        context =
            collectionSynced: @collection.synced
            requestsSubFilter: requestsSubFilter
            searchFilter: searchFilter

        if @lastRequestsFilter in ['active', 'paused']
            context.requests = _.filter(_.pluck(@collection.models, 'attributes'), (r) => not r.scheduled and not r.onDemand)
            context.requestsScheduled = _.filter(_.pluck(@collection.models, 'attributes'), (r) => r.scheduled)
            context.requestsOnDemand = _.filter(_.pluck(@collection.models, 'attributes'), (r) => r.onDemand)
            context.requests.reverse()
            context.requestsScheduled.reverse()
            context.requestsOnDemand.reverse()

        else
            context.requests = _.pluck(@collection.models, 'attributes')

        # Intersect starred requests before rendering
        for request in context.requests
            if app.collections.requestsStarred.get(request.name)?
                request.starred = true

        partials =
            partials:
                requestsBody: templateBody

        $search = @$el.find('input[type="search"]')
        searchWasFocused = $search.is(':focus')
        previousSearchTerm = $search.val()

        $requestsBodyContainer =  @$el.find('[data-requests-body-container]')

        if @lastRequestsFilter is 'active'
            partials.partials.requestsNav = templateNav
            $requestsNavContainer =  @$el.find('[data-requests-nav-container]')

        if not $requestsBodyContainer.length or forceFullRender
            @$el.html template(context, partials)

            if forceFullRender
                @$el.find('input[type="search"]').val(previousSearchTerm)
        else
            if @lastRequestsFilter is 'active'
                $requestsNavContainer.html templateNav context

            $requestsBodyContainer.html templateBody context

        @setupEvents()
        @setUpSearchEvents(refresh, searchWasFocused)
        utils.setupSortableTables()

        @

    setupEvents: ->
        @$el.find('[data-action="viewJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'request', $(e.target).data('request-id')

        @$el.find('[data-action="remove"]').unbind('click').on 'click', (e) =>
            $row = $(e.target).parents('tr')
            requestModel = @collection.get($(e.target).data('request-id'))

            vex.dialog.confirm
                message: @removeRequestTemplate(requestId: requestModel.get('id'))
                callback: (confirmed) =>
                    return unless confirmed
                    requestModel.destroy()
                    delete app.allRequests[requestModel.get('id')] # TODO - move to model on destroy?
                    @collection.remove(requestModel)
                    $row.remove()

        @$el.find('[data-action="deletePaused"]').unbind('click').on 'click', (e) =>
            $row = $(e.target).parents('tr')
            requestModel = @collection.get($(e.target).data('request-id'))

            vex.dialog.confirm
                message: "<p>Are you sure you want to delete the paused request:</p><pre>#{ requestModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    $row.remove()
                    requestModel.deletePaused().done =>
                        delete app.allRequests[requestModel.get('id')]
                        @collection.remove(requestModel)

        @$el.find('[data-action="unpause"]').unbind('click').on 'click', (e) =>
            $row = $(e.target).parents('tr')
            requestModel = @collection.get($(e.target).data('request-id'))

            vex.dialog.confirm
                message: "<p>Are you sure you want to delete the paused request:</p><pre>#{ requestModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    $row.remove()
                    requestModel.unpause().done =>
                        @render()

        @$el.find('[data-action="starToggle"]').unbind('click').on 'click', (e) =>
            $target = $(e.target)
            $table = $target.parents('table')

            requestName = $target.data('request-name')
            starred = $target.attr('data-starred') is 'true'

            app.collections.requestsStarred.toggle(requestName)
            $requests = $table.find("""[data-request-name="#{ requestName }"]""")

            if starred
                $requests.each -> $(@).attr('data-starred', 'false')
            else
                $requests.each -> $(@).attr('data-starred', 'true')

        @$el.find('[data-action="run-now"]').unbind('click').on 'click', (e) =>
            requestModel = new Request id: $(e.target).data('request-id')
            $row = $(e.target).parents('tr')

            requestType = $(e.target).data('request-type')

            vex.dialog.confirm
                message: "<p>Are you sure you want to run a task for this #{ requestType } request immediately:</p><pre>#{ requestModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    requestModel.run()
                    utils.flashRow $row

    setUpSearchEvents: (refresh, searchWasFocused) ->
        $search = @$el.find('input[type="search"]')

        if not app.isMobile and (not refresh or searchWasFocused)
            setTimeout -> $search.focus()

        $rows = @$('tbody > tr')

        lastText = ''

        $search.unbind().on 'change keypress paste focus textInput input click keydown', =>
            text = _.trim $search.val()

            if text is ''
                $rows.removeClass('filtered')
                app.router.navigate "/requests/#{ @lastRequestsFilter }/#{ @lastRequestsSubFilter }", { replace: true }

            if text isnt lastText
                @lastSearchFilter = text
                app.router.navigate "/requests/#{ @lastRequestsFilter }/#{ @lastRequestsSubFilter }/#{ @lastSearchFilter }", { replace: true }

                $rows.each ->
                    $row = $(@)

                    if not _.string.contains $row.data('request-id').toLowerCase(), text.toLowerCase()
                        $row.addClass('filtered')
                    else
                        $row.removeClass('filtered')

module.exports = RequestsView
