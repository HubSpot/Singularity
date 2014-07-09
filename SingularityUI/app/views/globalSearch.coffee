View = require './view'

class GlobalSearchView extends View

    lastSearchQuery: ''
    lastResponse: []

    template: require './templates/globalSearch'

    events: ->
        _.extend super,
            'click [data-close-global-search]': 'hide'

    initialize: ->
        $(window).on 'keydown', (event) =>
            return unless $(event.target).is 'body'
            if event.keyCode is 84 # t
                @show()
                event.preventDefault()
            if event.keyCode is 27 # ESC
                @hide()

        @inputEvent = _.debounce @inputEvent, 200

    render: ->
        @setElement @template()
        $('body').append @$el

        @setUpTypeahead()

    setUpTypeahead: ->
        @$('input').typeahead
            # Debounce event so we don't spam the server
            source: _.debounce (query, process) =>
                # Ignore empty queries
                return if not query
                # Use the same data if it's the same query
                if query is @lastSearchQuery
                    process @lastResponse
                    return

                @lastSearchQuery = query

                $.ajax
                    url: "#{ config.apiRoot }/history/requests/search"
                    data: requestIdLike: query

                    success: (response) =>
                        @lastResponse = response
                        process response
            , 200

            matcher: -> true
            highlighter: (item) -> item
            updater: (id) =>
                app.router.navigate "/request/#{ id }", { trigger: true }
                @toggle()

    reset: ->
        @$('input').val ''
        @$('ul').removeClass('dropdown-menu-hidden')
        @$('li').remove()

    show: ->
        @reset()
        @$el.parent().addClass 'global-search-active'
        @$('input').focus()

    hide: (event) ->
        if event?
            return if not $(event.target).attr('data-close-global-search')?

        @$el.parent().removeClass 'global-search-active'

    toggle: ->
        if @$el.parent().hasClass 'global-search-active' then @hide() else @show()


module.exports = GlobalSearchView
