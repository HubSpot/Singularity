View = require './view'

class RequestsView extends View

    templateRequestsActive: require './templates/requestsActive'
    templateRequestsPaused: require './templates/requestsPaused'
    templateRequestsPending: require './templates/requestsPending'
    templateRequestsCleaning: require './templates/requestsCleaning'

    initialize: ->
        @lastRequestsFilter = @options.requestsFilter

    fetch: ->
        if @lastRequestsFilter is 'active'
            @collection = app.collections.requestsActive

        if @lastRequestsFilter is 'paused'
            @collection = app.collections.requestsPaused

        if @lastRequestsFilter is 'pending'
            @collection = app.collections.requestsPending

        if @lastRequestsFilter is 'cleaning'
            @collection = app.collections.requestsCleaning

        @collection.fetch()

    refresh: ->
        return if @$el.find('input[type="search"]').val() isnt ''

        @fetch(@lastRequestsFilter).done =>
            @render(@lastRequestsFilter, refresh = true)

    render: (requestsFilter, refresh) =>
        @lastRequestsFilter = requestsFilter

        if @lastRequestsFilter is 'active'
            @collection = app.collections.requestsActive
            template = @templateRequestsActive

        if @lastRequestsFilter is 'paused'
            @collection = app.collections.requestsPaused
            template = @templateRequestsPaused

        if @lastRequestsFilter is 'pending'
            @collection = app.collections.requestsPending
            template = @templateRequestsPending

        if @lastRequestsFilter is 'cleaning'
            @collection = app.collections.requestsCleaning
            template = @templateRequestsCleaning

        context = {}

        if @lastRequestsFilter in ['active', 'paused']
            context.requests = _.filter(_.pluck(@collection.models, 'attributes'), (r) => not r.scheduled)
            context.requestsScheduled = _.filter(_.pluck(@collection.models, 'attributes'), (r) => r.scheduled)
            context.requests.reverse()
            context.requestsScheduled.reverse()

        else
            context.requests = _.pluck(@collection.models, 'attributes')

        # Intersect starred requests before rendering
        for request in context.requests
            if app.collections.requestsStarred.findWhere(name: request.name)?
                request.starred = true

        searchWasFocused = @$el.find('input[type="search"]').is(':focus')

        @$el.html template context

        @setupEvents()
        @setUpSearchEvents(refresh, searchWasFocused)
        utils.setupSortableTables()

        @

    setupEvents: ->
        @$el.find('[data-action="viewJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'request', $(e.target).data('request-id')

        $removeLinks = @$el.find('[data-action="remove"]')

        $removeLinks.unbind('click').on 'click', (e) =>
            $row = $(e.target).parents('tr')
            requestModel = @collection.get($(e.target).data('request-id'))

            vex.dialog.confirm
                message: "<p>Are you sure you want to delete the request:</p><pre>#{ requestModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    requestModel.destroy()
                    delete app.allRequests[requestModel.get('id')] # TODO - move to model on destroy?
                    @collection.remove(requestModel)
                    $row.remove()

        $deletePausedLinks = @$el.find('[data-action="deletePaused"]')

        $deletePausedLinks.unbind('click').on 'click', (e) =>
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

        $unpauseLinks = @$el.find('[data-action="unpause"]')

        $unpauseLinks.unbind('click').on 'click', (e) =>
            $row = $(e.target).parents('tr')
            requestModel = @collection.get($(e.target).data('request-id'))

            vex.dialog.confirm
                message: "<p>Are you sure you want to delete the paused request:</p><pre>#{ requestModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    $row.remove()
                    requestModel.unpause().done =>
                        @render()

        $starLinks = @$el.find('[data-action="starToggle"]')

        $starLinks.unbind('click').on 'click', (e) =>
            $target = $(e.target)
            $table = $target.parents('table')

            requestId = $target.data('request-id')
            requestName = $target.data('request-name')
            starred = $target.attr('data-starred') is 'true'

            app.collections.requestsStarred.toggle(requestName)
            $requests = $table.find("""[data-request-name="#{ requestName }"]""")

            if starred
                $requests.each -> $(@).attr('data-starred', 'false')
            else
                $requests.each -> $(@).attr('data-starred', 'true')

    setUpSearchEvents: (refresh, searchWasFocused) ->
        $search = @$el.find('input[type="search"]')

        if not app.isMobile and (not refresh or searchWasFocused)
            $search.focus()

        $rows = @$('tbody > tr')

        lastText = _.trim $search.val()

        $search.on 'change keypress paste focus textInput input click keydown', =>
            text = _.trim $search.val()

            if text is ''
                $rows.removeClass('filtered')

            if text isnt lastText
                $rows.each ->
                    $row = $(@)

                    if not _.string.contains $row.data('request-id').toLowerCase(), text.toLowerCase()
                        $row.addClass('filtered')
                    else
                        $row.removeClass('filtered')

module.exports = RequestsView