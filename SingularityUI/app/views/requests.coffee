View = require './view'

Request = require '../models/Request'

class RequestsView extends View

    templateRequestsActive: require './templates/requestsActive'
    templateRequestsActiveBody: require './templates/requestsActiveBody'
    templateRequestsActiveFilter: require './templates/requestsActiveFilter'

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

        if @lastRequestsFilter is 'active'
            @lastRequestsActiveSubFilter = @lastRequestsSubFilter

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
        return @ unless @ is app.views.current

        forceFullRender = requestsFilter isnt @lastRequestsFilter

        @lastRequestsFilter = requestsFilter
        @lastSearchFilter = searchFilter
        @$el.find('input[type="search"]').val searchFilter

        if @lastRequestsFilter is 'active'
            if @lastRequestsActiveSubFilter
                @lastRequestsSubFilter = @lastRequestsActiveSubFilter
        else
            @lastRequestsSubFilter = requestsSubFilter

        if @lastRequestsFilter is 'active'
            @collection = app.collections.requestsActive
            template = @templateRequestsActive
            templateBody = @templateRequestsActiveBody
            templateFilter = @templateRequestsActiveFilter

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

        if @lastRequestsFilter is 'paused'
            context.requests = _.filter(_.pluck(@collection.models, 'attributes'), (r) => not r.scheduled and not r.onDemand)
            context.requestsScheduled = _.filter(_.pluck(@collection.models, 'attributes'), (r) => r.scheduled)
            context.requests.reverse()
            context.requestsScheduled.reverse()

        if @lastRequestsFilter is 'active'
            if requestsSubFilter is 'running-on-demand-scheduled'
                context.requests = _.pluck(@collection.models, 'attributes')

            else
                filterFunction = => false

                if requestsSubFilter is 'running'
                    filterFunction = (r) => not r.scheduled and not r.onDemand

                if requestsSubFilter is 'running-on-demand'
                    filterFunction = (r) => not r.scheduled

                if requestsSubFilter is 'running-scheduled'
                    filterFunction = (r) => not r.onDemand

                if requestsSubFilter is 'on-demand'
                    filterFunction = (r) => r.onDemand

                if requestsSubFilter is 'on-demand-scheduled'
                    filterFunction = (r) => r.onDemand or r.scheduled

                if requestsSubFilter is 'scheduled'
                    filterFunction = (r) => r.scheduled

                context.requests = _.filter(_.pluck(@collection.models, 'attributes'), filterFunction)

            context.requests.reverse()

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

        $requestsBodyContainer =  @$el.find('[data-requests-body-container]')

        if @lastRequestsFilter is 'active'
            partials.partials.requestsFilter = templateFilter
            $requestsFilterContainer =  @$el.find('[data-requests-filter-container]')

        if not $requestsBodyContainer.length or forceFullRender
            @$el.html template(context, partials)

        else
            if @lastRequestsFilter is 'active'
                $requestsFilterContainer.html templateFilter context

            $requestsBodyContainer.html templateBody context

            @$el.find('input[type="search"]').val context.searchFilter

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

        @$el.find('[data-action="unpause"]').unbind('click').on 'click', (e) =>
            $row = $(e.target).parents('tr')
            requestModel = @collection.get($(e.target).data('request-id'))

            vex.dialog.confirm
                message: "<p>Are you sure you want to unpause the request?</p><pre>#{ requestModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    $row.remove()
                    requestModel.unpause().done =>
                        @render()

        @$el.find('[data-action="starToggle"]').unbind('click').on 'click', (e) =>
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

        @$el.find('[data-action="run-now"]').unbind('click').on 'click', (e) =>
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

            if requestType is 'on-demand'
                dialogType = vex.dialog.prompt
                dialogOptions.message += '<p>Additional command line input (optional):</p>'
            else
                dialogType = vex.dialog.confirm

            dialogType dialogOptions

        @$el.find('[data-requests-active-filter]').unbind('click').on 'click', (e) =>
            e.preventDefault()
            requestsActiveFilter = $(e.target).data('requests-active-filter')
            if e.metaKey or e.ctrlKey or e.shiftKey
                requestsActiveFilter = $(e.target).data('requests-active-filter-shift')
            @lastRequestsActiveSubFilter = requestsActiveFilter
            @lastSearchFilter = _.trim @$el.find('input[type="search"]').val()
            app.router.navigate "/requests/active/#{ requestsActiveFilter }/#{ @lastSearchFilter }", trigger: true

    setUpSearchEvents: (refresh, searchWasFocused) ->
        $search = @$el.find('input[type="search"]')

        if not app.isMobile and (not refresh or searchWasFocused)
            setTimeout -> $search.focus()

        $rows = @$('tbody > tr')

        previousLastSearchFilter = ''

        onChange = =>
            return unless @ is app.views.current

            @lastSearchFilter = _.trim $search.val()

            if @lastSearchFilter is ''
                $rows.removeClass('filtered')
                app.router.navigate "/requests/#{ @lastRequestsFilter }/#{ @lastRequestsSubFilter }", { replace: true }

            if previousLastSearchFilter isnt @lastSearchFilter
                app.router.navigate "/requests/#{ @lastRequestsFilter }/#{ @lastRequestsSubFilter }/#{ @lastSearchFilter }", { replace: true }
                previousLastSearchFilter = @lastSearchFilter

                $rows.each (i, row) =>
                    $row = $(row)

                    rowText = $row.data('request-id')
                    user = $row.data('request-deploy-user')
                    rowText = "#{ rowText } #{ user }" if user?

                    if utils.matchWordsInWords(@lastSearchFilter, rowText)
                        $row.removeClass('filtered')
                    else
                        $row.addClass('filtered')

            @$('table').each ->
                utils.handlePotentiallyEmptyFilteredTable $(@), 'request', @lastSearchFilter

        onChangeDebounced = _.debounce onChange, 200

        $search.unbind().on 'change keypress paste focus textInput input click keydown', onChangeDebounced

        if refresh
            onChange()

module.exports = RequestsView