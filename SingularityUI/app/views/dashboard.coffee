View = require './view'

class DashboardView extends View

    template: require './templates/dashboard'

    initialize: =>
        app.user.on 'change', @render, @

    render: (fromRoute) =>
        deployUser = app.user.get('deployUser')

        if not app.collections.requestsActive.synced
            app.collections.requestsActive.fetch().done => @render()

        context =
            collectionSycned: app.collections.requestsActive.synced
            requests: []
            requestsNotFound: []
            deployUser: deployUser

        # Intersect active requests before rendering
        for request in _.pluck(app.collections.requestsStarred.models, 'attributes')
            activeRequestModels = app.collections.requestsActive.filter (r) -> r.get('name') is request.name
            if activeRequestModels?
                _.each activeRequestModels, (activeRequestModel) ->
                    context.requests.push activeRequestModel.attributes
            else
                context.requestsNotFound.push request

        userRequestTotals = {}
        userRequests = app.collections.requestsActive.filter((r) -> r.get('deployUser') is deployUser)
        userRequestTotals.all = userRequests.length
        userRequestTotals.daemon = userRequests.filter((r) -> not r.get('scheduled') and not r.get('onDemand')).length
        userRequestTotals.onDemand = userRequests.filter((r) -> r.get('onDemand')).length
        userRequestTotals.scheduled = userRequests.filter((r) -> r.get('scheduled')).length

        _.extend context, { userRequestTotals }

        @$el.html @template context

        @setupEvents()
        utils.setupSortableTables()

        @

    setupEvents: ->
        @$el.find('[data-action="unstar"]').unbind('click').on 'click', (e) =>
            $target = $(e.target)
            $table = $target.parents('table')

            requestName = $target.data('request-name')

            app.collections.requestsStarred.toggle(requestName)

            $table.find("""[data-request-name="#{ requestName }"]""").each -> $(@).parents('tr').remove()

            if $table.find('tbody tr').length is 0
                @render()

        @$el.find('[data-requests-active-filter]').unbind('click').on 'click', (e) =>
            e.preventDefault()

            $link = $(e.target)
            $link = $(e.target).parents('a') if $(e.target).parents('a').length

            lastRequestsActiveSubFilter = $link.data('requests-active-filter')
            lastSearchFilter = app.user.get('deployUser')

            if app.views.requests?
                app.views.requests.lastRequestsActiveSubFilter = lastRequestsActiveSubFilter
                app.views.requests.lastSearchFilter = lastSearchFilter

            app.router.navigate "/requests/active/#{ lastRequestsActiveSubFilter }/#{ lastSearchFilter }", trigger: true

        @$el.find('[data-action="change-user"]').unbind('click').on 'click', (e) =>
            app.deployUserPrompt()

module.exports = DashboardView