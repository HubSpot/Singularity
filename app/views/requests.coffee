View = require './view'

class RequestsView extends View

    template: require './templates/requests'

    render: =>
        context =
            requests: app.collections.requests.toJSON()

        @$el.html @template context

        @setupEvents()
        @setUpSearchEvents()

    setupEvents: ->
        @$el.find('.view-json').unbind('click').click (event) ->
            utils.viewJSON (app.collections.requests.get $(event.target).data 'request-id').toJSON()

    setUpSearchEvents: =>
        $search = @$el.find('input[type="search"]').focus()
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