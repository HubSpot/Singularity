View = require './view'

class TailView extends View

    pollingTimeout: 3000

    template: require '../templates/tail'

    linesTemplate: require '../templates/tailLogLines'

    events: ->
        _.extend super,
            'click .tail-top-button': 'goToTop'
            'click .tail-bottom-button': 'goToBottom'

    initialize: ({@taskId, @path, firstRequest}) ->
        @filename = _.last @path.split '/'

        @listenTo @collection, 'reset',       @dumpContents
        @listenTo @collection, 'sync',        @renderLines
        @listenTo @collection, 'initialdata', @afterInitialData

        @listenTo @collection.state, 'change:moreToFetch', @showOrHideMoreToFetchSpinners
        @listenTo @collection.state, 'change:moreToFetchAtBeginning', @showOrHideMoreToFetchSpinners

        # For the visual loading indicator thing
        @listenTo @collection, 'request', =>
            @$el.addClass 'fetching-data'
        @listenTo @collection, 'sync', =>
            @$el.removeClass 'fetching-data'

    handleAjaxError: (response) =>
        # ATM we get 404s if we request dirs and 500s if the file doesn't exist
        if response.status in [404, 500]
            app.caughtError()
            @$el.html "<h1>Could not get request file.</h1>"

    render: =>
        breadcrumbs = utils.pathToBreadcrumbs @path

        @$el.html @template {@taskId, @filename, breadcrumbs}

        @$contents = @$ '.tail-contents'
        @$linesWrapper = @$contents.children('.lines-wrapper')

        # Attach scroll event manually because Backbone is poopy about it
        @$contents.on 'scroll', @handleScroll

        # Some stuff in the app can change this stuff. We wanna reset it
        $('html, body').css 'min-height', '0px'
        $('#global-zeroclipboard-html-bridge').css 'top', '1px'

    renderLines: ->
        # So we want to either prepend (fetchPrevious) or append (fetchNext) the lines
        # Well, or just render them if we're starting fresh
        $firstLine = @$linesWrapper.find '.line:first-child'
        $lastLine  = @$linesWrapper.find '.line:last-child'

        # If starting fresh
        if $firstLine.length is 0
            @$linesWrapper.html @linesTemplate lines: @collection.toJSON()
        else
            firstLineOffset = parseInt $firstLine.data 'offset'
            lastLineOffset  = parseInt $lastLine.data 'offset'
            # Prepending
            if @collection.getMinOffset() < firstLineOffset
                # Get only the new lines
                lines = @collection.filter (line) => line.get('offset') < firstLineOffset
                @$linesWrapper.prepend @linesTemplate lines: _.pluck lines, 'attributes'

                # Gonna need to scroll back to the previous `firstLine` after otherwise
                # we end up at the top again
                @$contents.scrollTop $firstLine.offset().top
            # Appending
            else if @collection.getStartOffsetOfLastLine() > lastLineOffset
                # Get only the new lines
                lines = @collection.filter (line) => line.get('offset') > lastLineOffset
                @$linesWrapper.append @linesTemplate lines: _.pluck lines, 'attributes'

    scrollToTop:    => @$contents.scrollTop 0
    scrollToBottom: =>
        scroll = => @$contents.scrollTop @$contents[0].scrollHeight
        scroll()

        # `preventFetch` will prevent the scroll-triggered fetch for
        # happening for 100 ms. This is to prevent a bug that can
        # happen if you have a REALLY busy log file
        @preventFetch = true
        setTimeout =>
            scroll()
            delete @preventFetch
        , 100

    # Get rid of all lines. Used when collection is reset
    dumpContents: -> @$linesWrapper.empty()

    handleScroll: (event) =>
        # `Debounce` on animation requests so we only do this when the
        # browser is ready for it
        if @frameRequest?
            cancelAnimationFrame @frameRequest

        @frameRequest = requestAnimationFrame =>
            scrollTop = @$contents.scrollTop()
            scrollHeight = @$contents[0].scrollHeight
            contentsHeight = @$contents.outerHeight()

            atBottom = scrollTop >= scrollHeight - contentsHeight
            atTop = scrollTop is 0

            if atBottom and not atTop
                if @collection.state.get('moreToFetch')
                    return if @preventFetch
                    @delayedFetchNext()
                else
                    @startTailing()
            else
                @stopTailing()

            if atTop and @collection.getMinOffset() isnt 0
                @delayedFetchPrevious()

    delayedFetchNext: ->
        if not @fetchNextTimeout
            @fetchNextTimeout = setTimeout =>
                @collection.fetchNext().always =>
                    @fetchNextTimeout = undefined
            , 0

    delayedFetchPrevious: ->
        if not @fetchPreviousTimeout
            @fetchPreviousTimeout = setTimeout =>
                @collection.fetchPrevious().always =>
                    @fetchPreviousTimeout = undefined
            , 0

    afterInitialData: =>
        setTimeout =>
            @scrollToBottom()
        , 150

        @startTailing()

    startTailing: =>
        return if @isTailing is true

        @isTailing = true
        @scrollToBottom()

        clearInterval @tailInterval
        @tailInterval = setInterval =>
            @collection.fetchNext().done =>
                # Only show the newly tail-ed lines if we are still tailing
                @scrollToBottom() if @isTailing
        , @pollingTimeout

        # The class is for CSS stylin' of certain stuff
        @$el.addClass 'tailing'

    stopTailing: ->
        return if @isTailing isnt true

        @isTailing = false
        clearInterval @tailInterval
        @$el.removeClass 'tailing'

    remove: ->
        clearInterval @tailInterval
        @$contents.off 'scroll'
        super

    goToTop: =>
        if @collection.getMinOffset() is 0
            @scrollToTop()
        else
            @collection.reset()
            @collection.fetchFromStart().done @scrollToTop

    goToBottom: =>
        if @collection.state.get('moreToFetch') is true
            @collection.reset()
            @collection.fetchInitialData()
        else
            @scrollToBottom()
            @startTailing()

    showOrHideMoreToFetchSpinners: (state) ->
        if state.changed.moreToFetchAtBeginning?
            @$('.tail-fetching-start').toggle(state.changed.moreToFetchAtBeginning)

        if state.changed.moreToFetch?
            @$('.tail-fetching-end').toggle(state.changed.moreToFetch)



module.exports = TailView
