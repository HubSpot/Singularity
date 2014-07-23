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
            @collection.currentPage -= 1
            return

        # For after the render
        haveButtons = @$('.table-subview-buttons').length

        @$el.html @template
            synced:  @collection.synced
            data:    _.pluck @collection.models, 'attributes'

        # Stop right here if we don't need to append the expand links and the buttons
        return if @collection.length isnt @collection.atATime and not haveButtons

        # Append expand / shrink link
        $header = @$('.page-header h1, .page-header h2, .page-header h3')
        if $header.length
            $header.find('small').remove()
            if not @expanded
                $header.append '<small class="hidden-xs"><a data-action="expand">more at once</a></small>'
            else if @expanded
                $header.append '<small class="hidden-xs"><a data-action="shrink">fewer at once</a></small>'

        # Append next / previous page buttons
        hasPrevButton = @collection.currentPage isnt 1
        hasNextButton = @collection.length is @collection.atATime

        @$el.append @buttonsTemplate {hasPrevButton, hasNextButton}

    nextPage: ->
        @collection.currentPage += 1 unless @collection.length isnt @collection.atATime
        @collection.fetch()

    previousPage: ->
        @collection.currentPage -= 1 unless @collection.currentPage is 1
        @collection.fetch()

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

        # A little padding
        arbitrarySpace = 10

        # Take away the stuff above and under the table from the size of the page
        availableSpace = pageHeight - spaceAboveTable - spaceUnderTable - arbitrarySpace
        # How many rows d'ya think we can fit in?
        canFit = Math.floor availableSpace / firstRowHeight

        # - 1 just in case
        @collection.atATime = canFit - 1
        @collection.currentPage = 1

        @collection.fetch().done =>
            @$('table').parent().css 'min-height', "#{ availableSpace }px"
            $(window).scrollTop @$el.offset().top - arbitrarySpace

    shrink: ->
        @expanded = false

        @$('table').parent().css 'min-height', 'auto'

        @collection.atATime = 5
        @collection.currentPage = 1
        @collection.fetch()

module.exports = ExpandableTableSubview