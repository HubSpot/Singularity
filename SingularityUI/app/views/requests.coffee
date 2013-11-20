View = require './view'

class RequestsView extends View

    templateRequestsActive: require './templates/requestsActive'
    templateRequestsPending: require './templates/requestsPending'
    templateRequestsCleaning: require './templates/requestsCleaning'

    render: (requestsFilter) =>
        @collection = app.collections.requestsActive
        template = @templateRequestsActive

        if requestsFilter is 'pending'
            @collection = app.collections.requestsPending
            template = @templateRequestsPending

        if requestsFilter is 'cleaning'
            @collection = app.collections.requestsCleaning
            template = @templateRequestsCleaning

        context =
            requests: @collection.toJSON()

        @$el.html template context

        @setupEvents()
        @setUpSearchEvents()
        utils.setupSortableTables()

    setupEvents: ->
        @$el.find('.view-json').unbind('click').click (event) ->
            utils.viewJSON 'request', $(event.target).data('request-id')

    setUpSearchEvents: =>
        $search = @$el.find('input[type="search"]')
        $search.focus() if $(window).width() > 568

        $rows = @$el.find('tbody > tr')

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