View = require './view'

class RequestsView extends View

    template: require './templates/requests'

    render: =>
        context =
            requests: app.collections.requests.toJSON()

        @$el.html @template context

        @setupEvents()
        # @setUpSearchEvents()

    setupEvents: ->
        @$el.find('.view-json').unbind('click').click (event) ->
            utils.viewJSON (app.collections.requests.get $(event.target).data 'request-id').toJSON()

    # setUpSearchEvents: =>
    #     @$search = @$el.find('input[type="search"]').focus()
    #
    #     lastText = _.trim @$search.val()

    #     @$search.on 'change keypress paste focus textInput input click keydown', =>
    #         text = _.trim @$search.val()

    #         if text is ''
    #             @$projects.find('.project').removeClass('filtered')

    #         if text isnt lastText
    #             @$projects.find('.project').each ->
    #                 $project = $(@)

    #                 if not _.string.contains $project.data('project').toLowerCase(), text.toLowerCase()
    #                     $project.addClass('filtered')

    #                     if $project.hasClass('opened-from-search opened')
    #                         $project.removeClass('opened-from-search opened')
    #                 else
    #                     $project.removeClass('filtered')

    #             $notFiltered = @$projects.find('.project:not(".filtered")')
    #             if $notFiltered.length is 1 and not $notFiltered.hasClass('opened')
    #                 $notFiltered.addClass('opened-from-search').click()

module.exports = RequestsView