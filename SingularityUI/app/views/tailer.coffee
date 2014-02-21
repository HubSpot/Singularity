View = require './view'

LogLines = require '../collections/LogLines'

class TailerView extends Backbone.View
    lineTemplate: require './templates/logline'

    readLength: 30000
    tailInterval: 3000

    events:
        'scroll': 'handleScroll'

    initialize: ->
        @tailing = null

        @nextPromise = Q {}
        @prevPromise = Q {}

        @lines = LogLines.getInstance @options

        @$container = @$('.tail-container')

        @lines.on 'sort', =>
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

    remove: =>
        @stopTailing()
        super

    scrollToBottom: =>
        @$el.scrollTop @el.scrollHeight

    renderLine: (model) =>
        @lineTemplate model.toJSON()

    fetchPrev: =>
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

        @prevPromise

    fetchNext: (offset=null) =>
        if @nextPromise.isFulfilled()
            @nextPromise = Q @lines.fetch
                data: $.param
                    offset: offset or @lines.getMaxOffset()
                    length: @readLength
                remove: false

        @nextPromise

    # if not already tailing, fetch more
    # if we're at the end, start tailing
    maybeStartTailing: =>
        if @tailing is null
            @fetchNext().then (data) =>
                if data.data is '\n'
                    @startTailing()

    startTailing: =>
        if @tailing is null
            @tailing = setInterval @tail, @tailInterval

    tail: (offset=null) =>
        @fetchNext(offset).then @scrollToBottom

    stopTailing: =>
        if @tailing isnt null
            clearInterval @tailing
            @tailing = null

    handleScroll: =>
        if not @$el.parents('html').length # Our view ain't in the page no mo
            return

        scrollTop = @$el.scrollTop()
        scrollBottom = scrollTop + @$el.height()
        scrollMax = @el.scrollHeight

        if scrollTop is 0 and @lines.getMinOffset() > 0
            # if at top, fetch previous lines
            @fetchPrev()
        else if scrollBottom is scrollMax
            # if at bottom, start tailing if appropriate
            @maybeStartTailing()
        else
            # if somewhere in the middle, stop tailing
            @stopTailing()

    render: =>
        @$el.addClass 'loading'

        # seek to the end of the file
        @lines.fetchEndOffset().then (offset) =>
            @tail Math.max(0, offset - @readLength)

        @

module.exports = TailerView