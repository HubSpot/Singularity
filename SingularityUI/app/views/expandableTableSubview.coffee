View = require './view'

# Reusable view for paginable tables
#
# You feed it a (server-side paginable) collection
# and a template and it works its magic
#
# If it's provided with a `.page-header h1` it can also be
# expanded to fit the entire page and shrunk back down after
class ExpandableTableSubview extends View

    buttonsTemplate: require '../templates/tableSubviewButtons'

    expanded: false

    # For having consistently sized tabled while chaging pages
    containerMinHeight: 0

    events: ->
        _.extend super,
            'click [data-action="next-page"]': 'nextPage'
            'click [data-action="previous-page"]': 'previousPage'
            'click [data-action="expand"]': 'expand'
            'click [data-action="shrink"]': 'startShrink'

    initialize: ({@collection, @template}) ->
        for eventName in ['sync', 'reset']
            @listenTo @collection, eventName, @render

    render: ->
        # If we've already rendered stuff and now we're trying to render
        # an empty collection (`next` returned an empty list)
        if not @collection.length and @collection.currentPage isnt 1
            console.log @collection.currentPage
            # Disable the next button and don't render anything
            $nextButton = @$('[data-action="next-page"]')
            $nextButton.attr 'disabled', true
            $nextButton.tooltip
                title:     'Nothing more!'
                placement: 'right'
                delay:
                    show: 100
                    hide: 2000
            $nextButton.tooltip 'show'
            setTimeout (=> $nextButton.tooltip 'hide'), 2000

            @collection.currentPage -= 1
            return undefined

        # For after the render
        haveButtons = @$('.table-subview-buttons').length

        @$el.html @template
            synced:  @collection.synced
            data:    _.pluck @collection.models, 'attributes'
            config: config

        @$('.actions-column a[title]').tooltip()

        @$('.table-container').css 'min-height', "#{ @containerMinHeight }px"

        haveMore = not (@collection.length isnt @collection.atATime and not haveButtons)

        # Append expand / shrink link
        $header = @$('.page-header h1, .page-header h2, .page-header h3')
        if $header.length
            $header.find('small').remove()
            if not @expanded and haveMore
                $header.append '<small class="hidden-xs"><a data-action="expand">more at once</a></small>'
            else if @expanded
                $header.append '<small class="hidden-xs"><a data-action="shrink">fewer at once</a></small>'

        # Stop right here if we don't need to append the buttons
        return if not haveMore

        # Append next / previous page buttons
        hasPrevButton = @collection.currentPage isnt 1
        hasNextButton = @collection.length is @collection.atATime

        @$el.append @buttonsTemplate {hasPrevButton, hasNextButton}

    nextPage: ->
        @collection.currentPage += 1 unless @collection.length isnt @collection.atATime
        @collection.fetch()

        # So the table doesn't shrink afterwards
        @containerMinHeight = @$('.table-container').height()

    previousPage: ->
        @collection.currentPage -= 1 unless @collection.currentPage is 1
        @collection.fetch()

    expand: ->
        @expanded = true

        utils.animatedExpansion @$el, @shrink

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

        # A little padding
        arbitrarySpace = 10

        # Take away the stuff above and under the table from the size of the page
        availableSpace = pageHeight - spaceAboveTable - spaceUnderTable - arbitrarySpace
        # How many rows d'ya think we can fit in?
        canFit = Math.floor availableSpace / firstRowHeight

        # - 1 just in case
        @collection.atATime = canFit - 1
        @collection.currentPage = 1

        @collection.fetch()

    startShrink: =>
        @$el.trigger 'shrink'
        @shrink()

    shrink: =>
        @expanded = false

        @$('.table-container').css 'min-height', '0px'
        @containerMinHeight = 0

        @collection.atATime = 5
        @collection.currentPage = 1
        @collection.fetch()

    flash: ->
        $(window).scrollTop @$el.offset().top
        @$el.addClass 'flash-background'
        setTimeout (=> @$el.removeClass 'flash-background'), 500

module.exports = ExpandableTableSubview