View = require './view'

class GlobalSearchView extends View

    lastSearchQuery: ''
    lastResponse: []

    template: require '../templates/globalSearch'

    events: ->
        _.extend super,
            'click [data-action="close-global-seach"]': 'hide'

    initialize: ->
        $(window).on 'keydown', (event) =>
            focusBody = $(event.target).is 'body'
            focusInput = $(event.target).is @$ 'input[type="search"]'

            modifierKey = event.metaKey or event.shiftKey
            sPressed = event.keyCode in [83, 84] and not modifierKey
            escPressed = event.keyCode is 27

            if escPressed and (focusBody or focusInput)
                @hide()
            else if sPressed and focusBody
                @show()
                event.preventDefault()

    render: ->
        @$el.html @template()

        @setUpTypeahead()

    setUpTypeahead: ->
        sourceFunction = (query, process) =>
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

        # Debounce event so we don't spam the server
        sourceFunction = _.debounce sourceFunction, 200

        @$('input').typeahead 
            highlight: true
        ,
            source: sourceFunction
            displayKey: (key) -> key

        @$('input').on 'typeahead:selected', (event, requestId) =>
            @hide()
            app.router.navigate "/request/#{ requestId }", { trigger: true }
            
    reset: ->
        @$('input').val ''
        @$('ul').removeClass 'dropdown-menu-hidden'
        @$('li').remove()

    show: ->
        @reset()
        @$el.parent().addClass 'global-search-active'
        @$('input').focus()

    hide: (event) ->
        if event?
            return if not $(event.target).data('action')? is 'close-global-seach'

            # Don't hide if you click the input box
            return if $(event.target).is('input')

        @$el.parent().removeClass 'global-search-active'

    toggle: ->
        if @$el.parent().hasClass 'global-search-active' then @hide() else @show()


module.exports = GlobalSearchView
