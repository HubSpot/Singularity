View = require './view'

TaskHistory = require '../models/TaskHistory'
LogLines = require '../collections/LogLines'

class TailerView extends Backbone.View
    lineTemplate: require './templates/logline'

    readLength: 30000
    tailInterval: 3000

    events:
        'scroll': 'handleScroll'

    initialize: ->
        @tailing = null

        @parent = @options.parent

        @nextPromise = Q {}
        @prevPromise = Q {}

        @lines = LogLines.getInstance @options

        @$container = @$('.tail-container')

        @taskHistory = new TaskHistory {}, taskId: @options.taskId

        @lines.on 'sort', =>
            @handleEmpty()

            children = @$container.children()

            # append all if tailer element is empty
            if children.length == 0
                @lines.each (model) =>
                    @$container.append @renderLine model
                return

            head = children.first()
            headIndex = @lines.indexOf(@lines.get(head.data('offset')))
            tailIndex = @lines.indexOf(@lines.get(children.last().data('offset')))

            origScrollHeight = @el.scrollHeight

            # add lines at top
            _.each @lines.first(headIndex), (model) =>
                head.before @renderLine model

            # update scroll
            if headIndex > 0
                scrollDiff = @el.scrollHeight - origScrollHeight
                @$el.scrollTop @$el.scrollTop() + scrollDiff

            # add lines at bottom
            _.each @lines.last(@lines.length - tailIndex - 1), (model) =>
                @$container.append @renderLine model

        # If the file isn't long enough to generate a sort,
        # starting tailing immediately
        @lines.once 'sort', =>
            if @$container.outerHeight() < @$el.outerHeight()
                @startTailing()

    remove: =>
        @stopTailing()
        super

    scrollToBottom: =>
        @$el.scrollTop @el.scrollHeight

    goToTop: =>
        @$container.empty()
        @lines.reset()
        
        # So we get an appropriate difference in order to
        #     get more content when we scroll down
        @lines.offset = -@readLength
        
        @stopTailing()
        
        promise = @fetchNext()
        promise.then =>
            @$el.scrollTop 0
            @stopTailing()

    goToBottom: =>
        @$container.empty()
        @lines.reset()
        
        # Gives us an appropriate line.difference
        #     so we can tail after the fetch
        @lines.offset = Infinity
        
        @seekToEnd()

    renderLine: (model) =>
        data = model.toJSON()
        data.data = $.trim data.data
        @lineTemplate data
        @lineTemplate model.toJSON()

    fetchPrev: =>
        @parent.$el.addClass('fetchPrev')

        # short circuit when we're at the top
        if @lines.getMinOffset() is 0
            return Q
                data: ''
                offset: 0

        if @prevPromise.isFulfilled()
            @prevPromise = Q @lines.fetch
                data: $.param
                    offset: Math.max(@lines.getMinOffset() - @readLength, 0)
                    length: Math.min(@lines.getMinOffset(), @readLength)
                remove: false

            @prevPromise.done =>
                @parent.$el.removeClass('fetchPrev')

        @prevPromise

    fetchNext: (offset = null) =>
        @parent.$el.addClass('fetchNext')

        if @nextPromise.isFulfilled()
            @nextPromise = Q @lines.fetch
                data: $.param
                    offset: offset or @lines.getMaxOffset()
                    length: @readLength
                remove: false

            @nextPromise.done =>
                @parent.$el.removeClass('fetchNext')

        @nextPromise

    startTailing: ->
        if @taskHistory.get('task').isStopped
            @stopTailing()
            return

        @parent.$el.addClass('tailing')

        if @tailing is null
            @tailing = setInterval (=> @tail()), @tailInterval

    tail: (offset = null) ->
        if app.views.current is @parent
            @fetchNext(offset).then @scrollToBottom

    stopTailing: ->
        @parent.$el.removeClass('tailing')

        if @tailing isnt null
            clearInterval @tailing
            @tailing = null

    handleScroll: =>
        if not @$el.parents('html').length # Our view ain't in the page no mo
            return
        
        # don't do anything if there's nothing to scroll through
        if @$container.is(":empty")
            return

        scrollTop = @$el.scrollTop()
        scrollBottom = scrollTop + @$el.height()
        scrollMax = @el.scrollHeight

        if scrollTop is 0 and @lines.getMinOffset() > 0
            # if at top, fetch previous lines
            @fetchPrev()
        else if scrollBottom >= scrollMax
            # If at the bottom, tail unless the last response
            #     gave us > 80% of how much we asked for
            # This way we can load content when scrolling down
            #     if we went to the top earlier
            if @lines.offsetDifference > @readLength * .8
                @fetchNext()
            else
                @startTailing()
        else
            # if somewhere in the middle, stop tailing
            @stopTailing()

    handleEmpty: (offset = @lines.offset) =>
        if @lines.length is 0 and offset is 0
            @$el.addClass 'empty'
            @startTailing()
        else
            @$el.removeClass 'empty'

    seekToEnd: ->
        @lines.fetchEndOffset().then (offset) =>
            @handleEmpty(offset)
            @tail Math.max(0, offset - @readLength)

    render: ->
        @$el.addClass 'loading'

        @lines.sort()

        @taskHistory.fetch().done =>
            @$el.removeClass 'loading'

            # seek to the end of the file
            @seekToEnd()

        @

module.exports = TailerView