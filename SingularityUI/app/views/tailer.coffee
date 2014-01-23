View = require './view'

LogLines = require '../collections/LogLines'

class TailerView extends Backbone.View
    lineTemplate: require './templates/logline'

    readLength: 30000

    initialize: ->
        @lines = LogLines.getInstance @options

        @lines.on 'sort', =>
            children = @$el.children()

            # append all if tailer element is empty
            if children.length == 0
                @lines.each (model) =>
                    @$el.append @renderLine model
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
                @$el.append @renderLine model

        # page / tail based on scroll events
        @$el.scroll @handleScroll

    scrollToBottom: =>
        @$el.scrollTop @el.scrollHeight

    renderLine: (model) =>
        @lineTemplate model.toJSON()

    page: =>
        if @lines.getMinOffset() is 0
            return

        @lines.fetch
            data: $.param
                offset: Math.max(@lines.getMinOffset() - @readLength, 0)
                length: Math.min(@lines.getMinOffset(), @readLength)
            remove: false

    tail: =>
        @lines.fetch
            data: $.param
                offset: @lines.getMaxOffset()
                length: @readLength
            remove: false

    handleScroll: =>
        scrollTop = @$el.scrollTop()
        scrollBottom = scrollTop + @$el.height()
        scrollMax = @el.scrollHeight

        if scrollTop is 0 and @lines.getMinOffset() > 0
            @page()
        else if scrollBottom is scrollMax
            @tail()  # TODO: keep attempting tail if we're at the bottom and theres no more data yet

    render: =>
        @$el.addClass 'loading'

        # seek to the end of the file
        @lines.fetchEndOffset().then (offset) =>
            ajaxPromise = @lines.fetch
                data: $.param
                    offset: Math.max(offset - @readLength, 0)
                    length: Math.min(offset, @readLength)

            ajaxPromise.done =>
                @$el.removeClass 'loading'
                @scrollToBottom()
                setTimeout @handleScroll

            ajaxPromise.fail (jqXHR, status, error) =>
                @$el.removeClass 'loading'

        @

module.exports = TailerView