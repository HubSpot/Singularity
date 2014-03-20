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

        scrollTop = @$el.scrollTop()
        scrollBottom = scrollTop + @$el.height()
        scrollMax = @el.scrollHeight

        if scrollTop is 0 and @lines.getMinOffset() > 0
            # if at top, fetch previous lines
            @fetchPrev()
        else if scrollBottom >= scrollMax
            # if at bottom, start tailing if appropriate
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

    render: ->
        @$el.addClass 'loading'

        @taskHistory.fetch().done =>
            @$el.removeClass 'loading'

            # seek to the end of the file
            @lines.fetchEndOffset().then (offset) =>
                @handleEmpty(offset)
                @tail Math.max(0, offset - @readLength)

        @

module.exports = TailerView