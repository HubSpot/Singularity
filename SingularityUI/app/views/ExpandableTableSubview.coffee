View = require './view'

# Reusable view for paginable tables
#
# You feed it a (server-side paginable) collection
# and a template and it works its magic
#
# If it's provided with a `.page-header h1` it can also be
# expanded to fit the entire page and shrunk back down after
class ExpandableTableSubview extends View

    buttonsTemplate: require './templates/tableSubviewButtons'

    atATime: 5
    currentPage: 1

    expanded: false

    events: ->
        _.extend super,
            'click [data-action="next-page"]': 'nextPage'
            'click [data-action="previous-page"]': 'previousPage'
            'click [data-action="expand"]': 'expand'
            'click [data-action="shrink"]': 'shrink'

    initialize: ({ @collection, @template }) ->
        @listenTo @collection, 'sync', @render

    render: ->
        # If we've already rendered stuff and now we're trying to render
        # an empty collection (`next` returned an empty list)
        if @collection.length is 0 and not @$el.is ':empty'
            # Disable the next button and don't render anything
            @$('[data-action="next-page"]').attr 'disabled', true
            @currentPage -= 1
            return

        @$el.html @template
            synced:  @collection.synced
            data:    _.pluck @collection.models, 'attributes'

        # Append expand / shrink link
        $header = @$('.page-header h1')
        if $header.length
            $header.find('small').remove()
            if not @expanded and @collection.length is @atATime
                $header.append '<small><a data-action="expand">more at once</a></small>'
            else if @expanded
                $header.append '<small><a data-action="shrink">fewer at once</a></small>'

        # Append next / previous page buttons
        hasPrevButton = @currentPage isnt 1
        hasNextButton = @collection.length is @atATime

        @$el.append @buttonsTemplate {hasPrevButton, hasNextButton}

    fetch: ->
        @collection.fetch
            data:
                count: @atATime
                page:  @currentPage
            reset: true

    nextPage: ->
        @currentPage += 1 unless @collection.length isnt @atATime
        @fetch()

    previousPage: ->
        @currentPage -= 1 unless @currentPage is 1
        @fetch()

    expand: ->
        @expanded = true

        # Container dimensions
        containerOffset = @$el.offset().top
        containerHeight = @$el.height()
        # Table dimensions
        $table = @$('table')
        tableOffset = $table.offset().top
        tableHeight = $table.height()

        # Figure out spaces
        spaceAboveTable = containerOffset - tableOffset
        spaceUnderTable = containerHeight - spaceAboveTable - tableHeight

        $firstRow = $ @$('tbody tr')[0]
        firstRowHeight = $firstRow.height()

        pageHeight = $(window).height()

        # Since it's fixed. 1.5 for a little margin
        navHeight = app.views.nav.$el.height() * 1.5
        # Take away the stuff above and under the table from the size of the page
        availableSpace = pageHeight - spaceAboveTable - spaceUnderTable - navHeight
        # How many rows d'ya think we can fit in?
        canFit = Math.floor availableSpace / firstRowHeight

        # - 1 just in case
        @atATime = canFit - 1
        @currentPage = 1

        @fetch().done =>
            $(window).scrollTop @$el.offset().top - navHeight

    shrink: ->
        @expanded = false

        @atATime = 5
        @currentPage = 1
        @fetch()

module.exports = ExpandableTableSubview