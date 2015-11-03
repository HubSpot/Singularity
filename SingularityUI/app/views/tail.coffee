View = require './view'

class TailView extends View

    pollingTimeout: 3000

    template: require '../templates/tail'

    linesTemplate: require '../templates/tailLogLines'

    events: ->
        _.extend super,
            'click .tail-top-button': 'goToTop'
            'click .tail-bottom-button': 'goToBottom'
            'click .offset-link' : 'offsetLink'

    initialize: ({@taskId, @path, @ajaxError, firstRequest, @offset}) ->
        @filename = _.last @path.split '/'

        @listenTo @collection, 'reset',       @dumpContents
        @listenTo @collection, 'sync',        @renderLines
        @listenTo @collection, 'initialOffsetData', @afterInitialOffsetData

        @listenTo @collection.state, 'change:moreToFetch', @showOrHideMoreToFetchSpinners
        @listenTo @collection.state, 'change:moreToFetchAtBeginning', @showOrHideMoreToFetchSpinners

        @listenTo @model, 'sync', @render

        # For the visual loading indicator thing
        @listenTo @collection, 'request', =>
            @$el.addClass 'fetching-data'
        @listenTo @collection, 'sync', =>
            @$el.removeClass 'fetching-data'

        @listenTo @model, 'change:isStillRunning', => @stopTailing() unless @model.get 'isStillRunning'

        @listenTo @model, 'change:isStillRunning', => @stopTailing() unless @model.get 'isStillRunning'

        @collectionRefreshInterval = null

        @listenTo @ajaxError, 'change:present', @render
        @listenTo @ajaxError, 'change:shouldRefresh', =>
            if @ajaxError.get('present') and @ajaxError.get('shouldRefresh')
                @collectionRefreshInterval = setInterval =>
                    @collection.fetchInitialData()
                , 2000


    render: =>
        breadcrumbs = utils.pathToBreadcrumbs @path
        @$el.html @template {@taskId, @filename, breadcrumbs, ajaxError: @ajaxError.toJSON(), taskHistory: @model.toJSON()}

        @$contents = @$ '.tail-contents'
        @$linesWrapper = @$contents.children('.lines-wrapper')

        # Attach scroll event manually because Backbone is poopy about it
        @$contents.on 'scroll, mousewheel', @handleScroll
        # FireFox support
        @$contents.on 'DOMMouseScroll', @handleScroll


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

            @$linesWrapper.html @linesTemplate
                lines: @collection.toJSON()
        else
            firstLineOffset = parseInt $firstLine.data 'offset'
            lastLineOffset  = parseInt $lastLine.data 'offset'
            # Prepending
            if @collection.getMinOffset() < firstLineOffset
                # Get only the new lines
                lines = @collection.filter (line) => line.get('offset') < firstLineOffset
                @$linesWrapper.prepend @linesTemplate
                    lines: _.pluck lines, 'attributes'

                # Gonna need to scroll back to the previous `firstLine` after otherwise
                # we end up at the top again
                @$contents.scrollTop $firstLine.offset().top
            # Appending
            else if @collection.getStartOffsetOfLastLine() > lastLineOffset
                # Get only the new lines
                lines = @collection.filter (line) => line.get('offset') > lastLineOffset
                @$linesWrapper.append @linesTemplate
                    lines: _.pluck lines, 'attributes'

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
        # Remove any `data is loading` message
        if @collectionRefreshInterval
            clearInterval @collectionRefreshInterval
        @dumpContents()

        setTimeout =>
            @scrollToBottom()
        , 150

        @startTailing()

    afterInitialOffsetData: =>
        setTimeout =>
            @$contents.scrollTop 1
            @$('.lines-wrapper').find('.line').first().addClass('highlightLine')
        , 150

    startTailing: =>
        return if @isTailing or not @model.get 'isStillRunning'

        @isTailing = true
        @scrollToBottom()

        clearInterval @tailInterval
        @tailInterval = setInterval =>
            @stopTailing() if not @model.get 'isStillRunning'

            @collection.fetchNext().done =>
                # Only show the newly tail-ed lines if we are still tailing
                @scrollToBottom() if @isTailing
        , @pollingTimeout

        # The class is for CSS stylin' of certain stuff
        @$el.addClass 'tailing'

    stopTailing: ->
        task = _.last @model.get('taskUpdates')
        return if @isTailing isnt true and task.taskState in utils.TERMINAL_TASK_STATES

        @isTailing = false
        clearInterval @tailInterval
        @$el.removeClass 'tailing'

    remove: ->
        @stopTailing()

        clearInterval @fetchNextTimeout
        clearInterval @fetchPreviousTimeout

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
        if state.changed.moreToFetchAtBeginning? and not @offset
            @$('.tail-fetching-start').toggle(state.changed.moreToFetchAtBeginning)

        if state.changed.moreToFetch?
            @$('.tail-fetching-end').toggle(state.changed.moreToFetch)

    offsetLink: (e) ->
        @$('.line').removeClass('highlightLine')
        $(e.currentTarget).closest('.line').addClass('highlightLine')


module.exports = TailView
