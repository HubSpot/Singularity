View = require './view'

class DashboardView extends View

    template: require './templates/dashboard'

    render: (fromRoute) =>
        if not app.collections.requestsActive.synced
            app.collections.requestsActive.fetch().done => @render()

        context =
            collectionSycned: app.collections.requestsActive.synced
            requests: []
            requestsNotFound: []

        # Intersect active requests before rendering
        for request in _.pluck(app.collections.requestsStarred.models, 'attributes')
            activeRequestModels = app.collections.requestsActive.filter (r) -> r.get('name') is request.name
            if activeRequestModels?
                _.each activeRequestModels, (activeRequestModel) ->
                    context.requests.push activeRequestModel.attributes
            else
                context.requestsNotFound.push request

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

module.exports = DashboardView