View = require './view'

class DashboardView extends View

    template: require './templates/dashboard'

    render: (fromRoute) =>
        context =
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
            $row = $(e.target).parents('tr')
            app.collections.requestsStarred.toggle($(e.target).data('request-id'))
            if not $row.siblings().length
                @render()
            else
                $row.remove()

module.exports = DashboardView