View = require './view'

class ExpandableTableSubview extends View

    atATime: 10
    currentPage: 0

    events: ->
        _.extend super,
            'click [data-action="next-page"]': 'nextPage'
            'click [data-action="previous-page"]': 'previousPage'
            'click [data-action="expand"]': 'expand'

    initialize: ({ @collection, @template }) ->
        @listenTo @collection, 'sync', @render

    render: ->
        @$el.html @template
            synced:  @collection.synced
            data:    _.pluck @collection.models, 'attributes'

    fetch: ->
        @collection.fetch
            data:
                count: @atATime
                page: @currentPage

    nextPage: ->
        @currentPage += 1
        @fetch()

    previousPage: ->
        @currentPage -= 1
        @fetch()

    expand: ->
        $firstRow = $(@$('tr')[0])
        firstRowHeight = $firstRow.height()
        firstRowOffset = $firstRow.offset().top

        pageHeight = $(window).height()

        availableSpace = pageHeight - firstRowOffset
        canFit = Math.floor availableSpace / firstRowHeight

        @atATime = canFit
        @currentPage = 0

        @fetch().done =>
            $(window).scrollTop @$('table').offset().top

module.exports = ExpandableTableSubview